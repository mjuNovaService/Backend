package com.mju.nova.backend.locker.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.response.PageRequests;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.locker.application.dto.AdminLockerAssignmentResponse;
import com.mju.nova.backend.locker.application.dto.AdminLockerResponse;
import com.mju.nova.backend.locker.application.dto.LockerAssignmentSearchCondition;
import com.mju.nova.backend.locker.application.dto.LockerBoardResponse;
import com.mju.nova.backend.locker.application.dto.MyLockerAssignmentResponse;
import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.Locker;
import com.mju.nova.backend.locker.domain.LockerApplyPeriod;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import com.mju.nova.backend.locker.domain.LockerAssignmentRepository;
import com.mju.nova.backend.locker.domain.LockerRepository;
import com.mju.nova.backend.locker.domain.LockerState;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.semester.domain.Semester;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사물함 조회 (Query). 응답 어디에도 비밀번호(암호문 포함)를 넣지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LockerQueryService {

    private final LockerRepository lockerRepository;
    private final LockerAssignmentRepository lockerAssignmentRepository;
    private final SemesterQueryService semesterQueryService;
    private final Clock clock;

    // ── 학생 ──

    /**
     * 공개 사물함 현황. 현재 학기가 없으면 학기·기간은 null, applyOpen은 false, 사용 가능한 사물함은 모두 AVAILABLE.
     * totalCount는 비활성 제외 사물함 수, availableCount는 그중 빈 칸 수 (KPI K5).
     */
    public LockerBoardResponse getBoard() {
        Optional<Semester> semester = semesterQueryService.findCurrent();
        LocalDateTime now = LocalDateTime.now(clock);
        Set<Long> inUseLockerIds = semester.map(current -> inUseLockerIds(current.getId())).orElse(Set.of());

        List<LockerBoardResponse.Item> lockers = lockerRepository.findAllByOrderByIdAsc().stream()
                .map(locker -> LockerBoardResponse.Item.of(locker, stateOf(locker, inUseLockerIds)))
                .toList();
        int totalCount = (int) lockers.stream().filter(item -> item.state() != LockerState.DISABLED).count();
        int availableCount = (int) lockers.stream().filter(item -> item.state() == LockerState.AVAILABLE).count();

        LocalDateTime applyStart = semester.map(Semester::getLockerApplyStart).orElse(null);
        LocalDateTime applyEnd = semester.map(Semester::getLockerApplyEnd).orElse(null);
        return new LockerBoardResponse(semester.map(Semester::getName).orElse(null), applyStart, applyEnd,
                LockerApplyPeriod.isOpen(applyStart, applyEnd, now), totalCount, availableCount, lockers);
    }

    // 지난 학기 미반납 배정도 보인다 (반납 가능). 최신 배정이 위로
    public List<MyLockerAssignmentResponse> getMyAssignments(String studentId) {
        Long currentSemesterId = semesterQueryService.findCurrent().map(Semester::getId).orElse(null);
        return lockerAssignmentRepository.findByStudent_IdOrderByIdDesc(studentId).stream()
                .map(assignment -> MyLockerAssignmentResponse.of(assignment, currentSemesterId))
                .toList();
    }

    // ── 관리자 ──

    /**
     * 사물함별 사용 여부와 그 학기 배정자. semesterId를 생략하면 현재 학기 (없으면 404 SE001).
     * 없는 semesterId면 배정이 없으니 모두 AVAILABLE·DISABLED로 보인다 (에러 없음).
     */
    public List<AdminLockerResponse> getAdminLockers(Long semesterId, LockerState state) {
        Long targetId = semesterId != null ? semesterId : currentSemester().getId();
        // 사물함마다 따로 조회하지 않고 그 학기 사용 중 배정을 학생까지 한 번에 가져온다
        Map<Long, LockerAssignment> inUse = lockerAssignmentRepository
                .findBySemester_IdAndStatus(targetId, AssignmentStatus.IN_USE).stream()
                .collect(Collectors.toMap(assignment -> assignment.getLocker().getId(), Function.identity(),
                        (first, second) -> first));

        return lockerRepository.findAllByOrderByIdAsc().stream()
                .map(locker -> {
                    LockerAssignment assignment = inUse.get(locker.getId());
                    return AdminLockerResponse.of(locker, LockerState.of(locker, assignment != null), assignment);
                })
                .filter(response -> state == null || response.state() == state)
                .toList();
    }

    /**
     * 배정 내역 (학기말 미반납 연락용). 배정 최신순. status는 IN_USE·RETURNED만 (CANCELED는 쓰지 않아 400 C001).
     * semesterId를 생략하면 현재 학기 (없으면 404 SE001). 없는 semesterId면 빈 페이지.
     */
    public PageResponse<AdminLockerAssignmentResponse> getAdminAssignments(LockerAssignmentSearchCondition condition,
                                                                           int page, int size) {
        if (condition.status() == AssignmentStatus.CANCELED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Long semesterId = condition.semesterId() != null ? condition.semesterId() : currentSemester().getId();
        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.of(lockerAssignmentRepository.findAll(searchFilter(condition, semesterId), pageable),
                AdminLockerAssignmentResponse::from);
    }

    private Semester currentSemester() {
        return semesterQueryService.findCurrent()
                .orElseThrow(() -> new BusinessException(ErrorCode.CURRENT_SEMESTER_NOT_FOUND));
    }

    // 필터는 모두 선택이고 AND
    private static Specification<LockerAssignment> searchFilter(LockerAssignmentSearchCondition condition,
                                                                Long semesterId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("semester").get("id"), semesterId));
            if (condition.status() != null) {
                predicates.add(cb.equal(root.get("status"), condition.status()));
            }
            if (condition.studentId() != null) {
                predicates.add(cb.equal(root.get("student").get("id"), condition.studentId()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Set<Long> inUseLockerIds(Long semesterId) {
        return new HashSet<>(lockerAssignmentRepository.findLockerIds(semesterId, AssignmentStatus.IN_USE));
    }

    private static LockerState stateOf(Locker locker, Set<Long> inUseLockerIds) {
        return LockerState.of(locker, inUseLockerIds.contains(locker.getId()));
    }
}

package com.mju.nova.backend.locker.application;

import com.mju.nova.backend.locker.application.dto.LockerBoardResponse;
import com.mju.nova.backend.locker.application.dto.MyLockerAssignmentResponse;
import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.Locker;
import com.mju.nova.backend.locker.domain.LockerApplyPeriod;
import com.mju.nova.backend.locker.domain.LockerAssignmentRepository;
import com.mju.nova.backend.locker.domain.LockerRepository;
import com.mju.nova.backend.locker.domain.LockerState;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.semester.domain.Semester;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
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

    private Set<Long> inUseLockerIds(Long semesterId) {
        return new HashSet<>(lockerAssignmentRepository.findLockerIds(semesterId, AssignmentStatus.IN_USE));
    }

    private static LockerState stateOf(Locker locker, Set<Long> inUseLockerIds) {
        return LockerState.of(locker, inUseLockerIds.contains(locker.getId()));
    }
}

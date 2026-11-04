package com.mju.nova.backend.locker.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.locker.application.dto.LockerAssignmentCreateResponse;
import com.mju.nova.backend.locker.application.dto.LockerPasswordChangeResponse;
import com.mju.nova.backend.locker.application.dto.LockerReturnResponse;
import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.Locker;
import com.mju.nova.backend.locker.domain.LockerApplyPeriod;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import com.mju.nova.backend.locker.domain.LockerAssignmentRepository;
import com.mju.nova.backend.locker.domain.LockerRepository;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.semester.domain.Semester;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사물함 변경 작업 (Command). 학생의 선착순 신청·비밀번호 수정·반납.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LockerCommandService {

    // schema.sql의 부분 유니크 인덱스 이름. 동시 신청이 코드 검사를 같이 통과해도 DB가 두 번째 저장을 막는다
    static final String STUDENT_ACTIVE_INDEX = "uk_locker_assignment_student_active";
    static final String LOCKER_ACTIVE_INDEX = "uk_locker_assignment_locker_active";

    private final LockerRepository lockerRepository;
    private final LockerAssignmentRepository lockerAssignmentRepository;
    private final StudentRepository studentRepository;
    private final SemesterQueryService semesterQueryService;
    private final LockerPasswordCipher lockerPasswordCipher;
    private final Clock clock;

    /**
     * 검사 순서는 명세 10번 시트 사물함 신청의 처리 규칙 순서 그대로.
     * 같은 사물함 경쟁은 사물함 행 잠금으로 줄을 세워 LC004, 같은 학생의 동시 신청은 부분 유니크 인덱스로 LC005.
     */
    public LockerAssignmentCreateResponse apply(String studentId, Long lockerId, String password) {
        LocalDateTime now = LocalDateTime.now(clock);
        Semester semester = semesterQueryService.findCurrent()
                .filter(current -> LockerApplyPeriod.isOpen(
                        current.getLockerApplyStart(), current.getLockerApplyEnd(), now))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCKER_APPLY_CLOSED));

        // 토큰은 유효한데 회원이 없거나 탈퇴했으면 M001 (대여와 같음)
        Student student = studentRepository.findById(studentId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Locker locker = lockerRepository.findByIdForUpdate(lockerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCKER_NOT_FOUND));
        if (!locker.isActive()) {
            throw new BusinessException(ErrorCode.LOCKER_DISABLED);
        }
        // 현재 학기 기준. 지난 학기 미반납 배정은 새 학기 신청을 막지 않는다
        if (lockerAssignmentRepository.existsByStudent_IdAndSemester_IdAndStatus(
                studentId, semester.getId(), AssignmentStatus.IN_USE)) {
            throw new BusinessException(ErrorCode.LOCKER_LIMIT_EXCEEDED);
        }
        if (lockerAssignmentRepository.existsByLocker_IdAndSemester_IdAndStatus(
                lockerId, semester.getId(), AssignmentStatus.IN_USE)) {
            throw new BusinessException(ErrorCode.LOCKER_ALREADY_TAKEN);
        }

        LockerAssignment assignment = LockerAssignment.assign(
                semester, student, locker, lockerPasswordCipher.encrypt(password));
        try {
            lockerAssignmentRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(conflictCode(e));
        }
        return LockerAssignmentCreateResponse.from(assignment);
    }

    // 새 비밀번호로 덮어쓴다. 예전 값은 복호화하지 않아서 키가 바뀐 뒤에도 다시 입력할 수 있다
    public LockerPasswordChangeResponse changePassword(String studentId, Long assignmentId, String password) {
        LockerAssignment assignment = findMyAssignmentForUpdate(studentId, assignmentId);
        assignment.changePassword(lockerPasswordCipher.encrypt(password));
        lockerAssignmentRepository.flush();   // 응답의 updatedAt(수정 시각)을 반영
        return LockerPasswordChangeResponse.from(assignment);
    }

    public LockerReturnResponse returnLocker(String studentId, Long assignmentId) {
        LockerAssignment assignment = findMyAssignmentForUpdate(studentId, assignmentId);
        assignment.returnLocker(LocalDateTime.now(clock));
        return LockerReturnResponse.from(assignment);
    }

    // 있는 배정인가? → 내 배정인가? (LC008은 엔티티가 검사)
    private LockerAssignment findMyAssignmentForUpdate(String studentId, Long assignmentId) {
        LockerAssignment assignment = lockerAssignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCKER_ASSIGNMENT_NOT_FOUND));
        if (!assignment.isOwnedBy(studentId)) {
            throw new BusinessException(ErrorCode.LOCKER_ASSIGNMENT_ACCESS_DENIED);
        }
        return assignment;
    }

    // 어느 부분 유니크 인덱스에 걸렸는지로 학생 쪽(LC005)·사물함 쪽(LC004)을 나눈다. 그 외 제약 위반은 그대로 500
    static ErrorCode conflictCode(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        if (message.contains(STUDENT_ACTIVE_INDEX)) {
            return ErrorCode.LOCKER_LIMIT_EXCEEDED;
        }
        if (message.contains(LOCKER_ACTIVE_INDEX)) {
            return ErrorCode.LOCKER_ALREADY_TAKEN;
        }
        throw e;
    }
}

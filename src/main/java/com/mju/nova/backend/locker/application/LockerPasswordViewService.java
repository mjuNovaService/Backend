package com.mju.nova.backend.locker.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.response.PageRequests;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.locker.application.dto.LockerPasswordAccessLogResponse;
import com.mju.nova.backend.locker.application.dto.LockerPasswordResponse;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import com.mju.nova.backend.locker.domain.LockerAssignmentRepository;
import com.mju.nova.backend.locker.domain.LockerPasswordAccessLog;
import com.mju.nova.backend.locker.domain.LockerPasswordAccessLogRepository;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회장단(PRESIDENT) 사물함 비밀번호 열람과 열람 기록. 회장단 확인은 SecurityConfig 규칙이 한다 (국장은 403 A102).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LockerPasswordViewService {

    private final LockerAssignmentRepository lockerAssignmentRepository;
    private final LockerPasswordAccessLogRepository lockerPasswordAccessLogRepository;
    private final StudentRepository studentRepository;
    private final LockerPasswordCipher lockerPasswordCipher;

    /**
     * 비밀번호를 복호화해 보여주고, 같은 트랜잭션에서 열람 기록을 남긴다 (기록 저장이 실패하면 열람도 실패).
     * 반납된 배정도 볼 수 있다. 복호화에 실패하면(키가 바뀜 등) 기록 없이 500.
     */
    public LockerPasswordResponse viewPassword(String viewerId, Long assignmentId) {
        LockerAssignment assignment = lockerAssignmentRepository.findWithLockerAndStudentById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCKER_ASSIGNMENT_NOT_FOUND));
        // 토큰은 유효한데 열람자 계정이 없거나 탈퇴했으면 M001
        Student viewer = studentRepository.findById(viewerId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        String password = lockerPasswordCipher.decrypt(assignment.getLockerPassword());
        lockerPasswordAccessLogRepository.save(LockerPasswordAccessLog.create(assignment, viewer));
        return LockerPasswordResponse.of(assignment, password);
    }

    // 열람 최신순. assignmentId가 있으면 그 배정의 기록만
    @Transactional(readOnly = true)
    public PageResponse<LockerPasswordAccessLogResponse> getAccessLogs(Long assignmentId, int page, int size) {
        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<LockerPasswordAccessLog> logs = assignmentId == null
                ? lockerPasswordAccessLogRepository.findAll(pageable)
                : lockerPasswordAccessLogRepository.findByAssignment_Id(assignmentId, pageable);
        return PageResponse.of(logs, LockerPasswordAccessLogResponse::from);
    }
}

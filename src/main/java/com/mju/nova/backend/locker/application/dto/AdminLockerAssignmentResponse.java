package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import java.time.LocalDateTime;

// 관리자 배정 목록 (학기말 미반납 연락용). 비밀번호는 넣지 않는다
public record AdminLockerAssignmentResponse(
        Long assignmentId,
        String semesterName,
        Long lockerId,
        String lockerNumber,
        String studentId,
        String studentName,
        String email,
        String phoneNum,
        AssignmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime returnedAt
) {
    public static AdminLockerAssignmentResponse from(LockerAssignment assignment) {
        return new AdminLockerAssignmentResponse(assignment.getId(), assignment.getSemester().getName(),
                assignment.getLocker().getId(), assignment.getLocker().getLockerNumber(),
                assignment.getStudent().getId(), assignment.getStudent().getName(),
                assignment.getStudent().getEmail(), assignment.getStudent().getPhoneNum(),
                assignment.getStatus(), assignment.getCreatedAt(), assignment.getReturnedAt());
    }
}

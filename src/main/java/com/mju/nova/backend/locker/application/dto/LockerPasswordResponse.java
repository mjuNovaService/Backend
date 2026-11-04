package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.LockerAssignment;

// 회장단 전용. 복호화한 비밀번호가 들어가는 유일한 응답
public record LockerPasswordResponse(
        Long assignmentId,
        String lockerNumber,
        String studentId,
        String password
) {
    public static LockerPasswordResponse of(LockerAssignment assignment, String password) {
        return new LockerPasswordResponse(assignment.getId(), assignment.getLocker().getLockerNumber(),
                assignment.getStudent().getId(), password);
    }
}

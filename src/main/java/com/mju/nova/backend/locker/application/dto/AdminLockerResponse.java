package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.Locker;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import com.mju.nova.backend.locker.domain.LockerState;
import java.time.LocalDateTime;

// 관리자 사물함 목록. assignment는 그 학기 IN_USE 배정 (없으면 null). 비밀번호는 넣지 않는다
public record AdminLockerResponse(
        Long lockerId,
        String lockerNumber,
        boolean lockerStatus,
        LockerState state,
        Assignment assignment
) {
    public static AdminLockerResponse of(Locker locker, LockerState state, LockerAssignment assignment) {
        return new AdminLockerResponse(locker.getId(), locker.getLockerNumber(), locker.isActive(), state,
                assignment == null ? null : Assignment.from(assignment));
    }

    public record Assignment(
            Long assignmentId,
            String studentId,
            String studentName,
            String email,
            String phoneNum,
            LocalDateTime createdAt
    ) {
        static Assignment from(LockerAssignment assignment) {
            return new Assignment(assignment.getId(), assignment.getStudent().getId(),
                    assignment.getStudent().getName(), assignment.getStudent().getEmail(),
                    assignment.getStudent().getPhoneNum(), assignment.getCreatedAt());
        }
    }
}

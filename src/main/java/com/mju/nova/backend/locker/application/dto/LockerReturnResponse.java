package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import java.time.LocalDateTime;

public record LockerReturnResponse(
        Long assignmentId,
        AssignmentStatus status,
        LocalDateTime returnedAt
) {
    public static LockerReturnResponse from(LockerAssignment assignment) {
        return new LockerReturnResponse(assignment.getId(), assignment.getStatus(), assignment.getReturnedAt());
    }
}

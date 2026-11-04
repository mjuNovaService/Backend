package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import java.time.LocalDateTime;

public record LockerAssignmentCreateResponse(
        Long assignmentId,
        Long lockerId,
        String lockerNumber,
        String semesterName,
        AssignmentStatus status,
        LocalDateTime createdAt
) {
    public static LockerAssignmentCreateResponse from(LockerAssignment assignment) {
        return new LockerAssignmentCreateResponse(assignment.getId(), assignment.getLocker().getId(),
                assignment.getLocker().getLockerNumber(), assignment.getSemester().getName(),
                assignment.getStatus(), assignment.getCreatedAt());
    }
}

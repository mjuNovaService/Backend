package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.LockerAssignment;
import java.time.LocalDateTime;

// 비밀번호는 넣지 않는다 (잊으면 운영진 문의)
public record MyLockerAssignmentResponse(
        Long assignmentId,
        String semesterName,
        boolean currentSemester,
        Long lockerId,
        String lockerNumber,
        AssignmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime returnedAt,
        boolean returnable
) {
    public static MyLockerAssignmentResponse of(LockerAssignment assignment, Long currentSemesterId) {
        return new MyLockerAssignmentResponse(assignment.getId(), assignment.getSemester().getName(),
                assignment.getSemester().getId().equals(currentSemesterId), assignment.getLocker().getId(),
                assignment.getLocker().getLockerNumber(), assignment.getStatus(), assignment.getCreatedAt(),
                assignment.getReturnedAt(), assignment.isInUse());
    }
}

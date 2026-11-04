package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.LockerAssignment;
import java.time.LocalDateTime;

// 비밀번호 수정 시각은 별도 컬럼 없이 updated_at
public record LockerPasswordChangeResponse(
        Long assignmentId,
        LocalDateTime updatedAt
) {
    public static LockerPasswordChangeResponse from(LockerAssignment assignment) {
        return new LockerPasswordChangeResponse(assignment.getId(), assignment.getUpdatedAt());
    }
}

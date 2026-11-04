package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.LockerPasswordAccessLog;
import java.time.LocalDateTime;

// 열람 시각은 별도 컬럼 없이 created_at
public record LockerPasswordAccessLogResponse(
        Long accessId,
        Long assignmentId,
        String lockerNumber,
        String ownerStudentId,
        String viewerStudentId,
        String viewerName,
        LocalDateTime viewedAt
) {
    public static LockerPasswordAccessLogResponse from(LockerPasswordAccessLog log) {
        return new LockerPasswordAccessLogResponse(log.getId(), log.getAssignment().getId(),
                log.getAssignment().getLocker().getLockerNumber(), log.getAssignment().getStudent().getId(),
                log.getViewer().getId(), log.getViewer().getName(), log.getCreatedAt());
    }
}

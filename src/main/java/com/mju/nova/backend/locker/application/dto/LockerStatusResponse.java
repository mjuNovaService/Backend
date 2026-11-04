package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.Locker;

public record LockerStatusResponse(
        Long lockerId,
        String lockerNumber,
        boolean lockerStatus
) {
    public static LockerStatusResponse from(Locker locker) {
        return new LockerStatusResponse(locker.getId(), locker.getLockerNumber(), locker.isActive());
    }
}

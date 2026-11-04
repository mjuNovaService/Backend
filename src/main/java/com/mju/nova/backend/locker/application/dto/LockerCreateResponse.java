package com.mju.nova.backend.locker.application.dto;

import java.util.List;

// 생성된 사물함 ID (보낸 순서)
public record LockerCreateResponse(
        List<Long> lockerIds
) {
}

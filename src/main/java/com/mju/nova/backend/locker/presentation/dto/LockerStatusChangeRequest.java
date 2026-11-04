package com.mju.nova.backend.locker.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record LockerStatusChangeRequest(
        // true 사용 가능, false 비활성
        @NotNull(message = "사용 여부를 선택해 주세요")
        Boolean lockerStatus
) {
}

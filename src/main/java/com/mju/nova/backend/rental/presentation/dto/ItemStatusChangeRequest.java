package com.mju.nova.backend.rental.presentation.dto;

import com.mju.nova.backend.rental.domain.ItemStatus;
import jakarta.validation.constraints.NotNull;

// NORMAL·DAMAGED·LOST가 아닌 값은 JSON 변환 단계에서 400 C001
public record ItemStatusChangeRequest(
        @NotNull(message = "상태를 입력해 주세요")
        ItemStatus status
) {
}

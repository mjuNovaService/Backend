package com.mju.nova.backend.slot.presentation.dto;

import jakarta.validation.constraints.NotNull;

// true: 열기, false: 닫기
public record SlotStatusChangeRequest(
        @NotNull(message = "열림 여부를 입력해 주세요")
        Boolean status
) {
}

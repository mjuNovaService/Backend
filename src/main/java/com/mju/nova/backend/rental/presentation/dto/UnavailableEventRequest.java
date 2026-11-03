package com.mju.nova.backend.rental.presentation.dto;

import com.mju.nova.backend.rental.domain.UnavailableReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// reason이 OUT_OF_STOCK·NO_SLOT가 아니면 JSON 변환 단계에서 400 C001
public record UnavailableEventRequest(
        @NotBlank(message = "물품명을 입력해 주세요")
        @Size(max = 50, message = "물품명은 50자 이하로 입력해 주세요")
        String itemName,

        @NotNull(message = "사유를 입력해 주세요")
        UnavailableReason reason
) {
}

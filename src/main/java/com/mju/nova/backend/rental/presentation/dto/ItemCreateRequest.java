package com.mju.nova.backend.rental.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemCreateRequest(
        @NotBlank(message = "물품명을 입력해 주세요")
        @Size(max = 50, message = "물품명은 50자 이하로 입력해 주세요")
        String itemName,

        @NotNull(message = "관리 번호를 입력해 주세요")
        @Min(value = 1, message = "관리 번호는 1 이상이어야 합니다")
        Integer itemNumber,

        @NotNull(message = "대여 일수를 입력해 주세요")
        @Min(value = 0, message = "대여 일수는 0 이상이어야 합니다")
        Integer rentalPeriodDay
) {
}

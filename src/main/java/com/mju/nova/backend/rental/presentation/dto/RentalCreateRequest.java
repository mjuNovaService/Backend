package com.mju.nova.backend.rental.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RentalCreateRequest(
        @NotBlank(message = "물품명을 입력해 주세요")
        @Size(max = 50, message = "물품명은 50자 이하로 입력해 주세요")
        String itemName,

        @NotNull(message = "수령 시간을 선택해 주세요")
        Long slotId,

        // yyyy-MM-dd. 형식이 틀리면 JSON 변환 단계에서 400 C001
        @NotNull(message = "수령 날짜를 선택해 주세요")
        LocalDate pickupDate,

        @Size(max = 200, message = "용도는 200자 이하로 입력해 주세요")
        String purpose
) {
}

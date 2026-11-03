package com.mju.nova.backend.rental.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 보낸 칸만 바꾼다 (null이면 그대로). 상태·대여 가능 여부는 물품 상태 변경 API로만 바꾼다.
 */
public record ItemUpdateRequest(
        @Size(max = 50, message = "물품명은 50자 이하로 입력해 주세요")
        @Pattern(regexp = ".*\\S.*", message = "물품명을 입력해 주세요")
        String itemName,

        @Min(value = 1, message = "관리 번호는 1 이상이어야 합니다")
        Integer itemNumber,

        @Min(value = 0, message = "대여 일수는 0 이상이어야 합니다")
        Integer rentalPeriodDay
) {
}

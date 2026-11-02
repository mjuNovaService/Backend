package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.ItemAvailability;

// 물품명별 수량. totalCount = 파손·분실을 뺀 보유 수량, availableCount = 지금 빌릴 수 있는 수량
public record ItemAvailabilityResponse(
        String itemName,
        Integer rentalPeriodDay,
        int totalCount,
        int availableCount
) {
    public static ItemAvailabilityResponse from(ItemAvailability availability) {
        return new ItemAvailabilityResponse(availability.name(), availability.rentalPeriodDay(),
                availability.totalCount().intValue(), availability.availableCount().intValue());
    }
}

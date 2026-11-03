package com.mju.nova.backend.rental.domain;

// 물품명별 수량 집계 결과 (ItemRepository.countByNameAndStatus)
public record ItemAvailability(
        String name,
        Integer rentalPeriodDay,
        Long totalCount,
        Long availableCount
) {
}

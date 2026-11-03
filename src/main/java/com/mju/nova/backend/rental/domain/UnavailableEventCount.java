package com.mju.nova.backend.rental.domain;

// 물품명·사유별 대여 불가 건수 (RentalUnavailableEventRepository.countByItemNameAndReason)
public record UnavailableEventCount(
        String itemName,
        UnavailableReason reason,
        Long count
) {
}

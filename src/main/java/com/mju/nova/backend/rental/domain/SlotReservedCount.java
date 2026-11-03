package com.mju.nova.backend.rental.domain;

// 슬롯별 RESERVED 대여 수 (RentRepository.countReservedBySlotIds)
public record SlotReservedCount(
        Long slotId,
        Long count
) {
}

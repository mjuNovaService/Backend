package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.RentStatus;
import java.time.LocalDate;

/**
 * 대여 이력 CSV 필터. 모두 선택이고 함께 쓰면 AND.
 * semesterId가 없으면 현재 학기, from·to는 수령일(pickupDate) 기준 기간 (양 끝 포함).
 */
public record RentalExportCondition(
        Long semesterId,
        String itemName,
        RentStatus status,
        LocalDate from,
        LocalDate to
) {
}

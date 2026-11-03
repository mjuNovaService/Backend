package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.RentStatus;
import java.time.LocalDate;

/**
 * 관리자 대여 목록 필터. 모두 선택이고 함께 쓰면 AND.
 * semesterId가 없으면 현재 학기, overdue는 true일 때만 연체 건으로 좁힌다.
 */
public record RentalSearchCondition(
        Long semesterId,
        LocalDate pickupDate,
        Long slotId,
        RentStatus status,
        String itemName,
        String studentId,
        Boolean overdue
) {
}

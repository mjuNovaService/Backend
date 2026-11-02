package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record RentalDetailResponse(
        Long rentId,
        Long itemId,
        String itemName,
        Integer itemNumber,
        Long slotId,
        DayOfWeek dayOfWeek,
        LocalDate pickupDate,
        LocalTime pickupStart,
        LocalTime pickupEnd,
        LocalDateTime dueAt,
        String purpose,
        RentStatus status,
        LocalDateTime returnedAt,
        boolean overdue,
        boolean cancelable,
        boolean returnable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RentalDetailResponse of(Rent rent, LocalDateTime now) {
        return new RentalDetailResponse(rent.getId(), rent.getItem().getId(), rent.getItem().getName(),
                rent.getItem().getItemNumber(), rent.getSlot().getId(), rent.getSlot().getDayOfWeek(),
                rent.getPickupDate(), rent.getSlot().getStartTime(), rent.getSlot().getEndTime(), rent.getDueAt(),
                rent.getPurpose(), rent.getStatus(), rent.getReturnedAt(),
                rent.isOverdue(now), rent.isCancelable(now), rent.isReturnable(),
                rent.getCreatedAt(), rent.getUpdatedAt());
    }
}

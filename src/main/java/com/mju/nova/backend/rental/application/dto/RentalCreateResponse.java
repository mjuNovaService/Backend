package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.slot.domain.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record RentalCreateResponse(
        Long rentId,
        Long itemId,
        String itemName,
        Integer itemNumber,
        Long slotId,
        LocalDate pickupDate,
        DayOfWeek dayOfWeek,
        LocalTime pickupStart,
        LocalTime pickupEnd,
        LocalDateTime dueAt,
        String purpose,
        RentStatus status,
        LocalDateTime createdAt
) {
    public static RentalCreateResponse from(Rent rent) {
        return new RentalCreateResponse(rent.getId(), rent.getItem().getId(), rent.getItem().getName(),
                rent.getItem().getItemNumber(), rent.getSlot().getId(), rent.getPickupDate(),
                rent.getSlot().getDayOfWeek(), rent.getSlot().getStartTime(), rent.getSlot().getEndTime(),
                rent.getDueAt(), rent.getPurpose(), rent.getStatus(), rent.getCreatedAt());
    }
}

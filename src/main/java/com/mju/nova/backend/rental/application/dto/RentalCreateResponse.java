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
        // TEMP(담당: A): 명세는 HH:mm인데 기본 Jackson은 LocalTime을 HH:mm:ss로 내보낸다. B는 따로 설정하지 않음 / A의 Jackson 전역 설정에 LocalTime HH:mm이 들어가면 이 주석만 지운다 (rental 응답 DTO 공통)
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

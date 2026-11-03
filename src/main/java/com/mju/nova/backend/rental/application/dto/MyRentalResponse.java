package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// 내 대여 목록 한 줄. cancelable·returnable로 프론트가 버튼을 보여줄지 정한다
public record MyRentalResponse(
        Long rentId,
        String itemName,
        Integer itemNumber,
        LocalDate pickupDate,
        LocalTime pickupStart,
        LocalTime pickupEnd,
        LocalDateTime dueAt,
        String purpose,
        RentStatus status,
        LocalDateTime returnedAt,
        boolean overdue,
        boolean cancelable,
        boolean returnable
) {
    public static MyRentalResponse of(Rent rent, LocalDateTime now) {
        return new MyRentalResponse(rent.getId(), rent.getItem().getName(), rent.getItem().getItemNumber(),
                rent.getPickupDate(), rent.getSlot().getStartTime(), rent.getSlot().getEndTime(), rent.getDueAt(),
                rent.getPurpose(), rent.getStatus(), rent.getReturnedAt(),
                rent.isOverdue(now), rent.isCancelable(now), rent.isReturnable());
    }
}

package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.Item;
import com.mju.nova.backend.rental.domain.ItemStatus;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.RentStatus;
import java.time.LocalDateTime;

// 관리자 물품 목록 한 줄. 진행 중 대여가 있으면 누가 빌렸는지 같이 보여준다 (없으면 "currentRent": null)
public record AdminItemResponse(
        Long itemId,
        String itemName,
        Integer itemNumber,
        ItemStatus status,
        Boolean isAvailable,
        Integer rentalPeriodDay,
        LocalDateTime updatedAt,
        LocalDateTime createdAt,
        CurrentRent currentRent
) {
    public static AdminItemResponse of(Item item, Rent activeRent, LocalDateTime now) {
        return new AdminItemResponse(item.getId(), item.getName(), item.getItemNumber(), item.getStatus(),
                item.getIsAvailable(), item.getRentalPeriodDay(), item.getUpdatedAt(), item.getCreatedAt(),
                activeRent == null ? null : CurrentRent.of(activeRent, now));
    }

    public record CurrentRent(
            Long rentId,
            String studentId,
            String studentName,
            RentStatus status,
            LocalDateTime dueAt,
            boolean overdue
    ) {
        static CurrentRent of(Rent rent, LocalDateTime now) {
            return new CurrentRent(rent.getId(), rent.getStudent().getId(), rent.getStudent().getName(),
                    rent.getStatus(), rent.getDueAt(), rent.isOverdue(now));
        }
    }
}

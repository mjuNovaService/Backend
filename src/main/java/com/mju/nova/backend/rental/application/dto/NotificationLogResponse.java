package com.mju.nova.backend.rental.application.dto;

import com.mju.nova.backend.rental.domain.NotificationLog;
import com.mju.nova.backend.rental.domain.NotificationType;
import com.mju.nova.backend.rental.domain.Rent;
import com.mju.nova.backend.rental.domain.SendStatus;
import java.time.LocalDateTime;

// 실패 사유는 ERD에 컬럼이 없어서 응답에 없다
public record NotificationLogResponse(
        Long notificationId,
        Long rentId,
        String studentId,
        String studentName,
        String itemName,
        Integer itemNumber,
        NotificationType type,
        SendStatus status,
        LocalDateTime createdAt
) {
    public static NotificationLogResponse from(NotificationLog notification) {
        Rent rent = notification.getRent();
        return new NotificationLogResponse(notification.getId(), rent.getId(), rent.getStudent().getId(),
                rent.getStudent().getName(), rent.getItem().getName(), rent.getItem().getItemNumber(),
                notification.getType(), notification.getStatus(), notification.getCreatedAt());
    }
}

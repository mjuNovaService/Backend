package com.mju.nova.backend.rental.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.rental.application.NotificationQueryService;
import com.mju.nova.backend.rental.application.dto.NotificationLogResponse;
import com.mju.nova.backend.rental.domain.NotificationType;
import com.mju.nova.backend.rental.domain.SendStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림 (관리자)", description = "반납 알림 발송 기록 조회")
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationAdminController {

    private final NotificationQueryService notificationQueryService;

    @Operation(summary = "알림 발송 기록", description = "최신순. status=FAILED로 실패한 알림을 확인한다. size 최대 50.")
    @GetMapping
    public ApiResponse<PageResponse<NotificationLogResponse>> getNotifications(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) SendStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notificationQueryService.getNotifications(type, status, date, page, size));
    }
}

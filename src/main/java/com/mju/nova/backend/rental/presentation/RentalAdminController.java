package com.mju.nova.backend.rental.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.rental.application.RentalQueryService;
import com.mju.nova.backend.rental.application.dto.AdminRentalResponse;
import com.mju.nova.backend.rental.application.dto.RentStatusHistoryResponse;
import com.mju.nova.backend.rental.application.dto.RentalSearchCondition;
import com.mju.nova.backend.rental.domain.RentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대여 현황. /api/admin/** 는 SecurityConfig에서 국장·회장단만 (Access Token의 role로 판단).
 */
@Tag(name = "대여 (관리자)", description = "대여 목록·상태 이력 조회")
@RestController
@RequestMapping("/api/admin/rentals")
@RequiredArgsConstructor
public class RentalAdminController {

    private final RentalQueryService rentalQueryService;

    @Operation(summary = "대여 목록",
            description = "semesterId를 생략하면 현재 학기. pickupDate + slotId로 그날 그 슬롯에 꺼내둘 물품을 본다. "
                    + "overdue=true면 연체 중인 건만. sort는 pickupDate·createdAt·dueAt 중 하나와 방향 (기본 pickupDate,desc).")
    @GetMapping
    public ApiResponse<PageResponse<AdminRentalResponse>> getRentals(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate pickupDate,
            @RequestParam(required = false) Long slotId,
            @RequestParam(required = false) RentStatus status,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "pickupDate,desc") String sort) {
        RentalSearchCondition condition = new RentalSearchCondition(
                semesterId, pickupDate, slotId, status, itemName, studentId, overdue);
        return ApiResponse.success(rentalQueryService.getRentals(condition, page, size, sort));
    }

    @Operation(summary = "대여 상태 이력", description = "상태가 바뀐 시각 순서대로. 최초 신청 이력의 beforeStatus는 null.")
    @GetMapping("/{rentId}/history")
    public ApiResponse<List<RentStatusHistoryResponse>> getHistory(@PathVariable Long rentId) {
        return ApiResponse.success(rentalQueryService.getHistory(rentId));
    }
}

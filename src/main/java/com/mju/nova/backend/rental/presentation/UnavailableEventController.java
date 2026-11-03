package com.mju.nova.backend.rental.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.rental.application.RentalUnavailableEventService;
import com.mju.nova.backend.rental.presentation.dto.UnavailableEventRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대여 불가 이벤트 기록 (KPI K4). 비로그인도 남길 수 있게 공개 (SecurityConfig에서 POST만 permitAll).
 * 계정 정보는 저장하지 않는다.
 */
@Tag(name = "대여 불가 이벤트", description = "빌리려 했지만 못 빌린 순간 기록 (공개)")
@RestController
@RequestMapping("/api/rentals/unavailable-events")
@RequiredArgsConstructor
public class UnavailableEventController {

    private final RentalUnavailableEventService rentalUnavailableEventService;

    @Operation(summary = "대여 불가 기록",
            description = "reason: OUT_OF_STOCK(재고 없음) | NO_SLOT(수령 시간 없음). 없는 물품명이면 404 I002. 응답 data 없음.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> record(@RequestBody @Valid UnavailableEventRequest request) {
        rentalUnavailableEventService.record(request.itemName(), request.reason());
        return ApiResponse.success();
    }
}

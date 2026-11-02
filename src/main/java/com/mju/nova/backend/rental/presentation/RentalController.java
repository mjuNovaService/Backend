package com.mju.nova.backend.rental.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.security.LoginStudent;
import com.mju.nova.backend.rental.application.RentalCommandService;
import com.mju.nova.backend.rental.application.RentalQueryService;
import com.mju.nova.backend.rental.application.dto.ItemAvailabilityResponse;
import com.mju.nova.backend.rental.application.dto.MyRentalResponse;
import com.mju.nova.backend.rental.application.dto.RentalCancelResponse;
import com.mju.nova.backend.rental.application.dto.RentalCreateResponse;
import com.mju.nova.backend.rental.application.dto.RentalDetailResponse;
import com.mju.nova.backend.rental.application.dto.RentalReturnResponse;
import com.mju.nova.backend.rental.domain.RentStatus;
import com.mju.nova.backend.rental.presentation.dto.RentalCreateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학생용 물품 대여. /api/rentals/** 는 SecurityConfig에서 로그인(Access Token) 필수.
 * 학생은 자기 대여만 볼 수 있다.
 */
@Tag(name = "대여", description = "대여 가능 수량·대여 신청·내 대여 조회·예약 취소·반납 (로그인 필요)")
@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalCommandService rentalCommandService;
    private final RentalQueryService rentalQueryService;

    @Operation(summary = "대여 가능 수량", description = "물품명별 보유 수량(파손·분실 제외)과 지금 빌릴 수 있는 수량.")
    @GetMapping("/availability")
    public ApiResponse<List<ItemAvailabilityResponse>> getAvailability() {
        return ApiResponse.success(rentalQueryService.getAvailability());
    }

    @Operation(summary = "대여 신청",
            description = "관리 번호는 서버가 배정한다. 수령 날짜는 오늘(평일) 또는 다음 평일. 현재 학기에 진행 중인 대여가 있으면 409 R008.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RentalCreateResponse> apply(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @RequestBody @Valid RentalCreateRequest request) {
        return ApiResponse.success(rentalCommandService.apply(loginStudent.studentId(), request.itemName(),
                request.slotId(), request.pickupDate(), request.purpose()));
    }

    // "/me"·"/availability"가 "/{rentId}"보다 구체적이라 Spring이 먼저 고른다
    @Operation(summary = "내 대여 목록", description = "수령 날짜 최신순. status로 거를 수 있다. 페이징 없음.")
    @GetMapping("/me")
    public ApiResponse<List<MyRentalResponse>> getMyRentals(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @RequestParam(required = false) RentStatus status) {
        return ApiResponse.success(rentalQueryService.getMyRentals(loginStudent.studentId(), status));
    }

    @Operation(summary = "대여 상세", description = "남의 대여면 403 R002.")
    @GetMapping("/{rentId}")
    public ApiResponse<RentalDetailResponse> getMyRental(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long rentId) {
        return ApiResponse.success(rentalQueryService.getMyRental(loginStudent.studentId(), rentId));
    }

    @Operation(summary = "예약 취소", description = "예약(RESERVED) 상태이고 수령 시작 전에만 가능 (아니면 409 R009).")
    @PatchMapping("/{rentId}/cancel")
    public ApiResponse<RentalCancelResponse> cancel(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long rentId) {
        return ApiResponse.success(rentalCommandService.cancel(loginStudent.studentId(), rentId));
    }

    @Operation(summary = "반납 완료", description = "대여 중(RENTED)일 때만 가능 (아니면 409 R010). 기한이 지나도 반납할 수 있다.")
    @PatchMapping("/{rentId}/return")
    public ApiResponse<RentalReturnResponse> returnItem(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long rentId) {
        return ApiResponse.success(rentalCommandService.returnItem(loginStudent.studentId(), rentId));
    }
}

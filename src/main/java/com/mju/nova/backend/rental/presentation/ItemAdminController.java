package com.mju.nova.backend.rental.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.rental.application.ItemCommandService;
import com.mju.nova.backend.rental.application.ItemQueryService;
import com.mju.nova.backend.rental.application.dto.AdminItemResponse;
import com.mju.nova.backend.rental.application.dto.ItemCreateResponse;
import com.mju.nova.backend.rental.application.dto.ItemResponse;
import com.mju.nova.backend.rental.domain.ItemStatus;
import com.mju.nova.backend.rental.presentation.dto.ItemCreateRequest;
import com.mju.nova.backend.rental.presentation.dto.ItemStatusChangeRequest;
import com.mju.nova.backend.rental.presentation.dto.ItemUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * 관리자 물품 관리. /api/admin/** 는 SecurityConfig에서 국장·회장단만.
 */
@Tag(name = "물품 (관리자)", description = "물품 목록·등록·정보 수정·상태 변경")
@RestController
@RequestMapping("/api/admin/items")
@RequiredArgsConstructor
public class ItemAdminController {

    private final ItemQueryService itemQueryService;
    private final ItemCommandService itemCommandService;

    @Operation(summary = "물품 목록", description = "물품명 가나다순, 관리 번호 오름차순. 진행 중 대여가 있으면 currentRent에 담긴다.")
    @GetMapping
    public ApiResponse<List<AdminItemResponse>> getItems(
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) Boolean isAvailable) {
        return ApiResponse.success(itemQueryService.getItems(itemName, status, isAvailable));
    }

    @Operation(summary = "물품 등록", description = "같은 물품명·관리 번호면 409 I003, 같은 물품명인데 대여 일수가 다르면 400 I005.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ItemCreateResponse> create(@RequestBody @Valid ItemCreateRequest request) {
        return ApiResponse.success(itemCommandService.create(
                request.itemName(), request.itemNumber(), request.rentalPeriodDay()));
    }

    @Operation(summary = "물품 정보 수정", description = "보낸 칸만 바뀐다. 대여 일수는 같은 물품명 전체에 적용된다.")
    @PatchMapping("/{itemId}")
    public ApiResponse<ItemResponse> update(@PathVariable Long itemId,
                                            @RequestBody @Valid ItemUpdateRequest request) {
        return ApiResponse.success(itemCommandService.update(
                itemId, request.itemName(), request.itemNumber(), request.rentalPeriodDay()));
    }

    @Operation(summary = "물품 상태 변경", description = "NORMAL·DAMAGED·LOST. 예약(RESERVED)이 걸린 물품이면 409 I004.")
    @PatchMapping("/{itemId}/status")
    public ApiResponse<ItemResponse> changeStatus(@PathVariable Long itemId,
                                                  @RequestBody @Valid ItemStatusChangeRequest request) {
        return ApiResponse.success(itemCommandService.changeStatus(itemId, request.status()));
    }
}

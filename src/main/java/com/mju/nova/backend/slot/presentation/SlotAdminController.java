package com.mju.nova.backend.slot.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.slot.application.SlotCommandService;
import com.mju.nova.backend.slot.application.SlotQueryService;
import com.mju.nova.backend.slot.application.dto.AdminSlotResponse;
import com.mju.nova.backend.slot.presentation.dto.SlotCreateRequest;
import com.mju.nova.backend.slot.presentation.dto.SlotStatusChangeRequest;
import com.mju.nova.backend.slot.presentation.dto.SlotUpdateRequest;
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
 * 관리자 수령 시간 관리. /api/admin/** 는 SecurityConfig에서 국장·회장단만.
 * 삭제 API는 없다 (대여 기록이 슬롯을 참조). 쓰지 않는 슬롯은 닫는다.
 */
@Tag(name = "수령 시간 (관리자)", description = "슬롯 목록·등록·수정·열고 닫기")
@RestController
@RequestMapping("/api/admin/slots")
@RequiredArgsConstructor
public class SlotAdminController {

    private final SlotQueryService slotQueryService;
    private final SlotCommandService slotCommandService;

    @Operation(summary = "슬롯 목록",
            description = "semesterId를 생략하면 현재 학기 (없으면 404 SE001). 닫힌 슬롯도 포함, reservedCount = 걸린 예약 수. 요일 → 시작 시각 순.")
    @GetMapping
    public ApiResponse<List<AdminSlotResponse>> getSlots(@RequestParam(required = false) Long semesterId) {
        return ApiResponse.success(slotQueryService.getSlots(semesterId));
    }

    @Operation(summary = "슬롯 등록",
            description = "현재 학기에 열린 상태로 등록 (현재 학기가 없으면 404 SE001). 같은 요일·시작 시각이면 409 SL004, 담당자가 운영진이 아니면 400 SL006.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminSlotResponse> create(@RequestBody @Valid SlotCreateRequest request) {
        return ApiResponse.success(slotCommandService.create(request.dayOfWeek(), request.startTime(),
                request.endTime(), request.managerStudentId()));
    }

    @Operation(summary = "슬롯 수정",
            description = "보낸 칸만 바뀐다. 예약(RESERVED)이 걸린 슬롯은 시각을 바꿀 수 없다 (409 SL007). 담당자 변경은 가능.")
    @PatchMapping("/{slotId}")
    public ApiResponse<AdminSlotResponse> update(@PathVariable Long slotId,
                                                 @RequestBody @Valid SlotUpdateRequest request) {
        return ApiResponse.success(slotCommandService.update(slotId, request.startTime(), request.endTime(),
                request.managerStudentId()));
    }

    @Operation(summary = "슬롯 열고 닫기", description = "닫아도 기존 예약은 그대로. 새 대여 신청만 막힌다 (400 SL002).")
    @PatchMapping("/{slotId}/status")
    public ApiResponse<AdminSlotResponse> changeStatus(@PathVariable Long slotId,
                                                       @RequestBody @Valid SlotStatusChangeRequest request) {
        return ApiResponse.success(slotCommandService.changeStatus(slotId, request.status()));
    }
}

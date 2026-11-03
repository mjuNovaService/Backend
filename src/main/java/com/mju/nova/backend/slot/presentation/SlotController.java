package com.mju.nova.backend.slot.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.slot.application.SlotQueryService;
import com.mju.nova.backend.slot.application.dto.SlotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학생용 수령 시간 조회. 대여 신청 화면에서 고를 슬롯 목록이라 로그인 없이 공개.
 */
@Tag(name = "수령 시간", description = "현재 학기 열린 수령 시간 조회 (공개)")
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotQueryService slotQueryService;

    @Operation(summary = "수령 시간 목록", description = "현재 학기의 열린 슬롯만. 요일 → 시작 시각 순.")
    @GetMapping
    public ApiResponse<List<SlotResponse>> getOpenSlots() {
        return ApiResponse.success(slotQueryService.getOpenSlots());
    }
}

package com.mju.nova.backend.rental.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.rental.application.RentalExportService;
import com.mju.nova.backend.rental.application.RentalQueryService;
import com.mju.nova.backend.rental.application.RentalUnavailableEventService;
import com.mju.nova.backend.rental.application.dto.AdminRentalResponse;
import com.mju.nova.backend.rental.application.dto.CsvFile;
import com.mju.nova.backend.rental.application.dto.RentStatusHistoryResponse;
import com.mju.nova.backend.rental.application.dto.RentalExportCondition;
import com.mju.nova.backend.rental.application.dto.RentalSearchCondition;
import com.mju.nova.backend.rental.application.dto.UnavailableEventSummaryResponse;
import com.mju.nova.backend.rental.domain.RentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대여 현황. /api/admin/** 는 SecurityConfig에서 국장·회장단만 (Access Token의 role로 판단).
 */
@Tag(name = "대여 (관리자)", description = "대여 목록·상태 이력 조회·CSV 내보내기·대여 불가 집계")
@RestController
@RequestMapping("/api/admin/rentals")
@RequiredArgsConstructor
public class RentalAdminController {

    private final RentalQueryService rentalQueryService;
    private final RentalExportService rentalExportService;
    private final RentalUnavailableEventService rentalUnavailableEventService;

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

    @Operation(summary = "대여 이력 CSV 내보내기",
            description = "KPI 집계용. semesterId를 생략하면 현재 학기(없으면 404 SE001), from·to는 수령일 기준 기간(양 끝 포함). "
                    + "페이지 없이 전체를 text/csv(UTF-8 BOM)로 내려준다. ApiResponse로 감싸지 않음.")
    // produces를 걸지 않는다: 걸면 C001 같은 실패 응답(JSON)이 Accept 협상에 걸릴 수 있어서 Content-Type만 직접 지정
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String itemName,
            @RequestParam(required = false) RentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        CsvFile file = rentalExportService.export(new RentalExportCondition(semesterId, itemName, status, from, to));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }

    @Operation(summary = "대여 불가 이벤트 집계",
            description = "KPI K4. from·to(양 끝 포함)를 생략하면 현재 학기 기간 (현재 학기가 없으면 404 SE001). 물품명·사유별 건수, 건수 내림차순.")
    @GetMapping("/unavailable-events/summary")
    public ApiResponse<UnavailableEventSummaryResponse> getUnavailableEventSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(rentalUnavailableEventService.summarize(from, to));
    }

    @Operation(summary = "대여 상태 이력", description = "상태가 바뀐 시각 순서대로. 최초 신청 이력의 beforeStatus는 null.")
    @GetMapping("/{rentId}/history")
    public ApiResponse<List<RentStatusHistoryResponse>> getHistory(@PathVariable Long rentId) {
        return ApiResponse.success(rentalQueryService.getHistory(rentId));
    }
}

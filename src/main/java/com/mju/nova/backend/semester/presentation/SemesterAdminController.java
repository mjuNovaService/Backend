package com.mju.nova.backend.semester.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.semester.application.SemesterCommandService;
import com.mju.nova.backend.semester.application.SemesterCommandService.SemesterIdResponse;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.semester.application.dto.SemesterResponse;
import com.mju.nova.backend.semester.presentation.dto.LockerApplyPeriodRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용. /api/admin/** 는 SecurityConfig에서 국장·회장단만 통과한다.
 * 학기 생성·전환 API는 없다 (날짜로 자동).
 */
@Tag(name = "학기 (관리자)", description = "학기 목록, 사물함 신청 기간 설정")
@RestController
@RequestMapping("/api/admin/semesters")
@RequiredArgsConstructor
public class SemesterAdminController {

    private final SemesterQueryService semesterQueryService;
    private final SemesterCommandService semesterCommandService;

    @Operation(summary = "학기 목록", description = "최근 학기가 위로. 지난 학기 대여 기록을 볼 때 학기를 고르는 용도.")
    @GetMapping
    public ApiResponse<List<SemesterResponse>> getAll() {
        return ApiResponse.success(semesterQueryService.getAll());
    }

    @Operation(summary = "현재 학기 사물함 신청 기간 설정",
            description = "시작 < 종료여야 한다 (400 SE002). 학기 시작 전(방학 중)으로 잡아도 된다. 일시 형식 yyyy-MM-dd'T'HH:mm:ss")
    @PatchMapping("/current/locker-period")
    public ApiResponse<SemesterIdResponse> changeLockerApplyPeriod(
            @RequestBody @Valid LockerApplyPeriodRequest request) {
        return ApiResponse.success(semesterCommandService.changeLockerApplyPeriod(
                request.lockerApplyStart(), request.lockerApplyEnd()));
    }
}

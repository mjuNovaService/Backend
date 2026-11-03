package com.mju.nova.backend.semester.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.semester.application.SemesterQueryService;
import com.mju.nova.backend.semester.application.dto.SemesterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "학기", description = "현재 학기 조회 (공개)")
@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterQueryService semesterQueryService;

    @Operation(summary = "현재 학기 조회",
            description = "학기 기간과 사물함 신청 기간. 1~6월은 그해 1학기, 7~12월은 그해 2학기 (방학은 다음 학기).")
    @GetMapping("/current")
    public ApiResponse<SemesterResponse> getCurrent() {
        return ApiResponse.success(semesterQueryService.getCurrent());
    }
}

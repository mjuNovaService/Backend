package com.mju.nova.backend.locker.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.common.security.LoginStudent;
import com.mju.nova.backend.locker.application.LockerPasswordViewService;
import com.mju.nova.backend.locker.application.dto.LockerPasswordAccessLogResponse;
import com.mju.nova.backend.locker.application.dto.LockerPasswordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회장단 전용 사물함 비밀번호 열람. SecurityConfig에서 두 경로 모두 PRESIDENT만 (국장은 403 A102).
 */
@Tag(name = "사물함 비밀번호 (회장단)", description = "학생이 비밀번호를 잊었을 때 열람, 열람 기록")
@RestController
@RequestMapping("/api/admin/lockers")
@RequiredArgsConstructor
public class LockerPasswordAdminController {

    private final LockerPasswordViewService lockerPasswordViewService;

    @Operation(summary = "사물함 비밀번호 열람",
            description = "회장단(PRESIDENT)만. 복호화한 비밀번호를 보여주고 볼 때마다 열람 기록을 남긴다. 반납된 배정도 볼 수 있다.")
    @GetMapping("/assignments/{assignmentId}/password")
    public ApiResponse<LockerPasswordResponse> viewPassword(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long assignmentId) {
        return ApiResponse.success(lockerPasswordViewService.viewPassword(loginStudent.studentId(), assignmentId));
    }

    @Operation(summary = "비밀번호 열람 기록", description = "회장단(PRESIDENT)만. 열람 최신순. assignmentId로 한 배정의 기록만 볼 수 있다.")
    @GetMapping("/password-access-logs")
    public ApiResponse<PageResponse<LockerPasswordAccessLogResponse>> getAccessLogs(
            @RequestParam(required = false) Long assignmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(lockerPasswordViewService.getAccessLogs(assignmentId, page, size));
    }
}

package com.mju.nova.backend.locker.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.security.LoginStudent;
import com.mju.nova.backend.locker.application.LockerCommandService;
import com.mju.nova.backend.locker.application.LockerQueryService;
import com.mju.nova.backend.locker.application.dto.LockerAssignmentCreateResponse;
import com.mju.nova.backend.locker.application.dto.LockerBoardResponse;
import com.mju.nova.backend.locker.application.dto.LockerPasswordChangeResponse;
import com.mju.nova.backend.locker.application.dto.LockerReturnResponse;
import com.mju.nova.backend.locker.application.dto.MyLockerAssignmentResponse;
import com.mju.nova.backend.locker.presentation.dto.LockerApplyRequest;
import com.mju.nova.backend.locker.presentation.dto.LockerPasswordChangeRequest;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학생용 사물함. GET /api/lockers(현황)만 비로그인 공개, 나머지 /api/lockers/** 는 SecurityConfig에서 로그인 필수.
 * 신청 취소는 없고, 반납은 학생 본인만 한다. 신청 기간 설정은 학기 API(PATCH /api/admin/semesters/current/locker-period).
 */
@Tag(name = "사물함", description = "사물함 현황(공개)·선착순 신청·내 사물함·비밀번호 수정·반납 (로그인 필요)")
@RestController
@RequestMapping("/api/lockers")
@RequiredArgsConstructor
public class LockerController {

    private final LockerCommandService lockerCommandService;
    private final LockerQueryService lockerQueryService;

    @Operation(summary = "사물함 현황",
            description = "현재 학기 사물함 그리드(lockerId 오름차순)와 남은 개수, 신청 기간. 로그인 없이 호출 가능. 누가 쓰는지는 보여주지 않는다.")
    @GetMapping
    public ApiResponse<LockerBoardResponse> getBoard() {
        return ApiResponse.success(lockerQueryService.getBoard());
    }

    @Operation(summary = "사물함 신청",
            description = "선착순 즉시 확정. password는 숫자 4자리 문자열. 신청 기간이 아니면 400 LC003, "
                    + "이미 배정된 사물함이면 409 LC004, 이번 학기에 사용 중인 사물함이 있으면 409 LC005.")
    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LockerAssignmentCreateResponse> apply(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @RequestBody @Valid LockerApplyRequest request) {
        return ApiResponse.success(
                lockerCommandService.apply(loginStudent.studentId(), request.lockerId(), request.password()));
    }

    @Operation(summary = "내 사물함", description = "배정 최신순. 지난 학기 미반납 배정도 보인다. 비밀번호는 응답에 없다.")
    @GetMapping("/me")
    public ApiResponse<List<MyLockerAssignmentResponse>> getMyAssignments(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent) {
        return ApiResponse.success(lockerQueryService.getMyAssignments(loginStudent.studentId()));
    }

    @Operation(summary = "사물함 비밀번호 수정", description = "사용 중(IN_USE)인 내 배정만 (아니면 409 LC008). 숫자 4자리.")
    @PatchMapping("/assignments/{assignmentId}/password")
    public ApiResponse<LockerPasswordChangeResponse> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long assignmentId,
            @RequestBody @Valid LockerPasswordChangeRequest request) {
        return ApiResponse.success(
                lockerCommandService.changePassword(loginStudent.studentId(), assignmentId, request.password()));
    }

    @Operation(summary = "사물함 반납",
            description = "사용 중(IN_USE)인 내 배정만 (아니면 409 LC008). 지난 학기 미반납도 반납할 수 있다. 사물함은 바로 빈 칸이 된다.")
    @PatchMapping("/assignments/{assignmentId}/return")
    public ApiResponse<LockerReturnResponse> returnLocker(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long assignmentId) {
        return ApiResponse.success(lockerCommandService.returnLocker(loginStudent.studentId(), assignmentId));
    }
}

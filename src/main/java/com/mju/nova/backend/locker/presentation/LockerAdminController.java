package com.mju.nova.backend.locker.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.locker.application.LockerCommandService;
import com.mju.nova.backend.locker.application.LockerQueryService;
import com.mju.nova.backend.locker.application.dto.AdminLockerAssignmentResponse;
import com.mju.nova.backend.locker.application.dto.AdminLockerResponse;
import com.mju.nova.backend.locker.application.dto.LockerAssignmentSearchCondition;
import com.mju.nova.backend.locker.application.dto.LockerCreateResponse;
import com.mju.nova.backend.locker.application.dto.LockerStatusResponse;
import com.mju.nova.backend.locker.domain.AssignmentStatus;
import com.mju.nova.backend.locker.domain.LockerState;
import com.mju.nova.backend.locker.presentation.dto.LockerCreateRequest;
import com.mju.nova.backend.locker.presentation.dto.LockerStatusChangeRequest;
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
 * 관리자 사물함 관리. /api/admin/** 는 SecurityConfig에서 국장·회장단만 (Access Token의 role로 판단).
 * 관리자 반납 처리는 없다 (반납은 학생만). 신청 기간 설정은 학기 API(PATCH /api/admin/semesters/current/locker-period).
 */
@Tag(name = "사물함 (관리자)", description = "사물함 목록·등록·사용 여부 변경, 배정 목록")
@RestController
@RequestMapping("/api/admin/lockers")
@RequiredArgsConstructor
public class LockerAdminController {

    private final LockerCommandService lockerCommandService;
    private final LockerQueryService lockerQueryService;

    @Operation(summary = "사물함 목록",
            description = "lockerId 오름차순. semesterId를 생략하면 현재 학기(없으면 404 SE001). "
                    + "state(AVAILABLE·IN_USE·DISABLED)로 거를 수 있다. assignment는 그 학기 사용 중 배정 (없으면 null).")
    @GetMapping
    public ApiResponse<List<AdminLockerResponse>> getLockers(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) LockerState state) {
        return ApiResponse.success(lockerQueryService.getAdminLockers(semesterId, state));
    }

    @Operation(summary = "사물함 등록",
            description = "번호 1~200개를 보낸 순서대로 등록 (앞뒤 공백은 잘라서 저장). "
                    + "요청 안 중복이나 이미 등록된 번호가 하나라도 있으면 아무것도 저장하지 않고 409 LC009.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LockerCreateResponse> register(@RequestBody @Valid LockerCreateRequest request) {
        return ApiResponse.success(lockerCommandService.register(request.lockerNumbers()));
    }

    @Operation(summary = "사물함 사용 여부 변경",
            description = "false면 비활성(고장·사용 중지). 현재 학기 사용 중 배정이 있으면 비활성화할 수 없다 (409 LC010).")
    @PatchMapping("/{lockerId}/status")
    public ApiResponse<LockerStatusResponse> changeStatus(
            @PathVariable Long lockerId,
            @RequestBody @Valid LockerStatusChangeRequest request) {
        return ApiResponse.success(lockerCommandService.changeStatus(lockerId, request.lockerStatus()));
    }

    @Operation(summary = "사물함 배정 목록",
            description = "배정 최신순. semesterId를 생략하면 현재 학기(없으면 404 SE001). "
                    + "학기말에 status=IN_USE로 미반납 학생을 찾는다. status는 IN_USE·RETURNED, studentId는 완전 일치.")
    @GetMapping("/assignments")
    public ApiResponse<PageResponse<AdminLockerAssignmentResponse>> getAssignments(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) String studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(lockerQueryService.getAdminAssignments(
                new LockerAssignmentSearchCondition(semesterId, status, studentId), page, size));
    }
}

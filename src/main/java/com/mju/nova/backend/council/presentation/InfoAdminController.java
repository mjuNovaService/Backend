package com.mju.nova.backend.council.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.security.LoginStudent;
import com.mju.nova.backend.council.application.InfoCommandService;
import com.mju.nova.backend.council.application.dto.InfoIdResponse;
import com.mju.nova.backend.council.presentation.dto.InfoCreateRequest;
import com.mju.nova.backend.council.presentation.dto.InfoUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용. /api/admin/** 는 SecurityConfig에서 국장·회장단(DIRECTOR, PRESIDENT)만 통과한다.
 */
@Tag(name = "학생회 소개 (관리자)", description = "소개 글 등록·수정·삭제")
@RestController
@RequestMapping("/api/admin/infos")
@RequiredArgsConstructor
public class InfoAdminController {

    private final InfoCommandService infoCommandService;

    @Operation(summary = "소개 글 등록", description = "작성한 관리자가 updatedBy로 기록된다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InfoIdResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @RequestBody @Valid InfoCreateRequest request) {
        return ApiResponse.success(infoCommandService.create(
                loginStudent.studentId(), request.type(), request.title(), request.content()));
    }

    @Operation(summary = "소개 글 수정", description = "보낸 칸만 바뀐다. 수정한 관리자가 updatedBy로 기록된다.")
    @PatchMapping("/{infoId}")
    public ApiResponse<InfoIdResponse> update(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long infoId,
            @RequestBody @Valid InfoUpdateRequest request) {
        return ApiResponse.success(infoCommandService.update(
                loginStudent.studentId(), infoId, request.type(), request.title(), request.content()));
    }

    @Operation(summary = "소개 글 삭제")
    @DeleteMapping("/{infoId}")
    public ApiResponse<Void> delete(@PathVariable Long infoId) {
        infoCommandService.delete(infoId);
        return ApiResponse.success();
    }
}

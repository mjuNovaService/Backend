package com.mju.nova.backend.council.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.security.LoginStudent;
import com.mju.nova.backend.council.application.QuestionCommandService;
import com.mju.nova.backend.council.application.QuestionQueryService;
import com.mju.nova.backend.council.application.dto.MyQuestionDetailResponse;
import com.mju.nova.backend.council.application.dto.MyQuestionSummaryResponse;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.council.application.dto.QuestionIdResponse;
import com.mju.nova.backend.council.presentation.dto.QuestionCreateRequest;
import com.mju.nova.backend.council.presentation.dto.QuestionUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * 학생용 Q&A. /api/questions/** 는 SecurityConfig에서 로그인(Access Token) 필수.
 * 학생은 자기 질문만 볼 수 있다.
 */
@Tag(name = "Q&A", description = "질문 등록·내 질문 조회·수정·삭제 (로그인 필요)")
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionCommandService questionCommandService;
    private final QuestionQueryService questionQueryService;

    @Operation(summary = "질문 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuestionIdResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @RequestBody @Valid QuestionCreateRequest request) {
        return ApiResponse.success(questionCommandService.create(
                loginStudent.studentId(), request.title(), request.content()));
    }

    // "/me"가 "/{questionId}"보다 구체적이라 Spring이 먼저 고른다
    @Operation(summary = "내 질문 목록", description = "최신순. page는 0부터, size는 최대 50.")
    @GetMapping("/me")
    public ApiResponse<PageResponse<MyQuestionSummaryResponse>> getMyQuestions(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(questionQueryService.getMyQuestions(loginStudent.studentId(), page, size));
    }

    @Operation(summary = "내 질문 상세", description = "답변이 있으면 answer에 같이 담긴다. 남의 질문이면 403 QA002.")
    @GetMapping("/{questionId}")
    public ApiResponse<MyQuestionDetailResponse> getMyQuestion(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long questionId) {
        return ApiResponse.success(questionQueryService.getMyQuestion(loginStudent.studentId(), questionId));
    }

    @Operation(summary = "내 질문 수정", description = "보낸 칸만 바뀐다. 답변이 달린 질문은 수정할 수 없다 (409 QA006).")
    @PatchMapping("/{questionId}")
    public ApiResponse<QuestionIdResponse> update(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long questionId,
            @RequestBody @Valid QuestionUpdateRequest request) {
        return ApiResponse.success(questionCommandService.update(
                loginStudent.studentId(), questionId, request.title(), request.content()));
    }

    @Operation(summary = "내 질문 삭제", description = "답변이 달린 질문은 삭제할 수 없다 (409 QA003).")
    @DeleteMapping("/{questionId}")
    public ApiResponse<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long questionId) {
        questionCommandService.delete(loginStudent.studentId(), questionId);
        return ApiResponse.success();
    }
}

package com.mju.nova.backend.council.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.security.LoginStudent;
import com.mju.nova.backend.council.application.AnswerCommandService;
import com.mju.nova.backend.council.application.QuestionQueryService;
import com.mju.nova.backend.council.application.dto.AdminQuestionDetailResponse;
import com.mju.nova.backend.council.application.dto.AdminQuestionSummaryResponse;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.council.application.dto.QuestionIdResponse;
import com.mju.nova.backend.council.presentation.dto.AnswerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Q&A (관리자)", description = "전체 질문 조회·답변 등록·수정")
@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
public class QuestionAdminController {

    private final QuestionQueryService questionQueryService;
    private final AnswerCommandService answerCommandService;

    @Operation(summary = "전체 질문 목록",
            description = "answered 생략=전체, true=답변 완료, false=답변 대기. 최신순, size 최대 50.")
    @GetMapping
    public ApiResponse<PageResponse<AdminQuestionSummaryResponse>> getAllQuestions(
            @RequestParam(required = false) Boolean answered,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(questionQueryService.getAllQuestions(answered, page, size));
    }

    @Operation(summary = "질문 상세", description = "작성자 학번·이름과 답변(없으면 null)을 같이 준다.")
    @GetMapping("/{questionId}")
    public ApiResponse<AdminQuestionDetailResponse> getQuestion(@PathVariable Long questionId) {
        return ApiResponse.success(questionQueryService.getQuestionForAdmin(questionId));
    }

    @Operation(summary = "답변 등록", description = "질문 하나에 답변 하나. 이미 있으면 409 QA004 → 수정 API 사용.")
    @PostMapping("/{questionId}/answer")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuestionIdResponse> createAnswer(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long questionId,
            @RequestBody @Valid AnswerRequest request) {
        return ApiResponse.success(answerCommandService.create(loginStudent.studentId(), questionId, request.content()));
    }

    @Operation(summary = "답변 수정", description = "아직 답변이 없으면 404 QA005.")
    @PatchMapping("/{questionId}/answer")
    public ApiResponse<QuestionIdResponse> updateAnswer(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable Long questionId,
            @RequestBody @Valid AnswerRequest request) {
        return ApiResponse.success(answerCommandService.update(loginStudent.studentId(), questionId, request.content()));
    }
}

package com.mju.nova.backend.member.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.member.application.EmailVerificationCommandService;
import com.mju.nova.backend.member.application.dto.EmailSendResponse;
import com.mju.nova.backend.member.application.dto.EmailVerifyResponse;
import com.mju.nova.backend.member.presentation.dto.EmailSendRequest;
import com.mju.nova.backend.member.presentation.dto.EmailVerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이메일 인증", description = "회원가입용 학교 이메일 인증")
@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationCommandService emailVerificationCommandService;

    @Operation(summary = "인증코드 발송", description = "학교 이메일(@mju.ac.kr)로 6자리 인증코드를 보낸다. 5분 동안 유효, 60초 안에 재요청 불가.")
    @PostMapping("/send")
    public ApiResponse<EmailSendResponse> send(@RequestBody @Valid EmailSendRequest request) {
        return ApiResponse.success(emailVerificationCommandService.send(request.email()));
    }

    @Operation(summary = "인증코드 확인", description = "가장 최근에 받은 인증코드를 확인한다. 이미 인증한 이메일이면 그대로 성공.")
    @PostMapping("/verify")
    public ApiResponse<EmailVerifyResponse> verify(@RequestBody @Valid EmailVerifyRequest request) {
        return ApiResponse.success(emailVerificationCommandService.verify(request.email(), request.code()));
    }
}

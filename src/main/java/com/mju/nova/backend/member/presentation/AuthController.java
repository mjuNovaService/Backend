package com.mju.nova.backend.member.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.member.application.AuthCommandService;
import com.mju.nova.backend.member.application.dto.LoginResult;
import com.mju.nova.backend.member.presentation.dto.LoginRequest;
import com.mju.nova.backend.member.presentation.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "회원가입·로그인·토큰")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandService authCommandService;
    private final RefreshTokenCookieFactory cookieFactory;

    @Operation(summary = "로그인",
            description = "학번과 비밀번호로 로그인한다. Access Token은 Body로, Refresh Token은 HttpOnly 쿠키로 발급한다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        // 1. Service에 로그인 맡기기
        LoginResult result = authCommandService.login(request.studentId(), request.password());

        // 2. Refresh Token은 쿠키로, 나머지는 Body로 나눠 담기
        ResponseCookie cookie = cookieFactory.create(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(LoginResponse.from(result)));
    }
}

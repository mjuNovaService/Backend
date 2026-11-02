package com.mju.nova.backend.member.presentation.dto;

import com.mju.nova.backend.member.application.dto.LoginResult;

/**
 * 로그인 응답 Body. Refresh Token은 여기 넣지 않고 쿠키로 보냄.
 */
public record LoginResponse(
        String accessToken,
        String studentId,
        String name,
        String role
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.accessToken(), result.studentId(), result.name(), result.role());
    }
}

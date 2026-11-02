package com.mju.nova.backend.member.presentation;

import com.mju.nova.backend.common.security.JwtProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 쿠키를 만듦. 로그인·재발급(발급)과 로그아웃(삭제)에서 같이 씀.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";   // 재발급·로그아웃 요청에만 붙도록

    private final JwtProperties jwtProperties;

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken)
                .maxAge(jwtProperties.refreshTokenExpiration())   // 14일
                .build();
    }

    // 같은 이름·같은 Path로 Max-Age=0을 보내면 브라우저가 지운다
    public ResponseCookie delete() {
        return base("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)                                   // JavaScript가 읽지 못함 (XSS 방어)
                .secure(jwtProperties.cookieSecure())             // HTTPS에서만 전송
                .sameSite(jwtProperties.cookieSameSite())         // 다른 사이트의 요청에는 안 붙음 (CSRF 방어)
                .path(COOKIE_PATH);
    }
}

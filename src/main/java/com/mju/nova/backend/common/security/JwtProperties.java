package com.mju.nova.backend.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 jwt.* 설정값을 담는다.
 * 예) jwt.access-token-expiration: 30m → accessTokenExpiration = 30분
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,                    // 토큰 서명용 비밀키 (Base64)
        Duration accessTokenExpiration,   // Access Token 수명
        Duration refreshTokenExpiration,  // Refresh Token 수명
        boolean cookieSecure,             // Refresh Token 쿠키 Secure 속성
        String cookieSameSite             // Refresh Token 쿠키 SameSite 속성
) {
}

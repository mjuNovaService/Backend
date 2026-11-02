package com.mju.nova.backend.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT를 만들고 검증.
 * Access Token  payload: sub=학번, role, type="access"
 * Refresh Token payload: sub=학번, type="refresh", jti(고유 번호)
 * 고유번호는 혹시 모를 연속 요청에 같은 토큰이 생성되는걸 막기 위해 추가했습니다.
 */
@Component
public class JwtProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtProvider(JwtProperties properties) {
        // Base64 비밀키 → HMAC-SHA 서명 키 (HS256 이상은 32바이트 이상 필요)
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.accessTokenExpiration = properties.accessTokenExpiration();
        this.refreshTokenExpiration = properties.refreshTokenExpiration();
    }

    public String createAccessToken(String studentId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(studentId)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenExpiration.toMillis()))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(String studentId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(studentId)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                // 같은 학생이 같은 초에 두 번 로그인해도 토큰이 겹치지 않게 고유 번호를 추가함.
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenExpiration.toMillis()))
                .signWith(key)
                .compact();
    }

    /**
     * Access Token을 검증하고 로그인 정보를 꺼냄.
     * 서명이 틀렸거나, 만료됐거나, Refresh Token을 잘못 넣었으면 JwtException.
     */
    public LoginStudent parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Access Token이 아닙니다");
        }
        return new LoginStudent(claims.getSubject(), claims.get(CLAIM_ROLE, String.class));
    }

    /**
     * Refresh Token을 검증하고 학번을 꺼냄. (토큰 재발급에서 사용)
     */
    public String parseRefreshToken(String token) {
        Claims claims = parse(token);
        if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("Refresh Token이 아닙니다");
        }
        return claims.getSubject();
    }

    public Duration getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    // 서명·만료를 확인하고 payload를 꺼냄
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

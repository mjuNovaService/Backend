package com.mju.nova.backend.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Refresh Token을 DB에 원문 대신 SHA-256 해시(64자)로 저장하기 위한 도구.
 * 같은 토큰은 항상 같은 해시가 나오므로 WHERE token_hash = ? 로 찾을 수 있음.
 */
public final class TokenHasher {

    private TokenHasher() {
    }

    public static String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다", e);
        }
    }
}

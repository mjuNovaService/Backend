package com.mju.nova.backend.member.application.dto;

/**
 * AuthCommandService.login()의 결과.
 * refreshToken은 Controller가 쿠키에 담고, 나머지는 응답 Body로 나감.
 */
public record LoginResult(
        String accessToken,
        String refreshToken,
        String studentId,
        String name,
        String role
) {
}

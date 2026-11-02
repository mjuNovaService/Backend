package com.mju.nova.backend.member.application.dto;

/**
 * AuthCommandService.reissue()의 결과. 새로 발급한 두 토큰.
 * refreshToken은 Controller가 쿠키에 담고, accessToken은 응답 Body로 나감.
 */
public record ReissueResult(
        String accessToken,
        String refreshToken
) {
}

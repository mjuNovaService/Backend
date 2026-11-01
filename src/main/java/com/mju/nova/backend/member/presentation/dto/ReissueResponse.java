package com.mju.nova.backend.member.presentation.dto;

import com.mju.nova.backend.member.application.dto.ReissueResult;

/**
 * 재발급 응답 Body. 새 Refresh Token은 쿠키로 보내므로 여기 넣지 않음.
 */
public record ReissueResponse(String accessToken) {

    public static ReissueResponse from(ReissueResult result) {
        return new ReissueResponse(result.accessToken());
    }
}

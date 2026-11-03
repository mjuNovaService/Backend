package com.mju.nova.backend.member.application.dto;

/**
 * 인증코드 확인 응답. 성공하면 verified는 항상 true.
 */
public record EmailVerifyResponse(String email, boolean verified) {

    public static EmailVerifyResponse of(String email) {
        return new EmailVerifyResponse(email, true);
    }
}

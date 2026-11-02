package com.mju.nova.backend.member.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 auth.email.* 설정값.
 */
@ConfigurationProperties(prefix = "auth.email")
public record EmailVerificationProperties(
        String allowedDomain,       // 허용 도메인 (mju.ac.kr)
        Duration codeExpiration,    // 인증코드 유효 시간 (5분)
        Duration resendCooldown,    // 재발송 대기 시간 (60초)
        Duration signupWindow       // 인증 완료 후 회원가입 허용 시간 (30분)
) {
}

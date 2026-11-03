package com.mju.nova.backend.member.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 auth.signup.* 설정값.
 */
@ConfigurationProperties(prefix = "auth.signup")
public record SignupProperties(
        String allowedCollege,      // 재학증명서 '대학' (공백·점 뺀 이름)
        String initialAdminEmail,   // 최초 관리자(회장) 이메일. 비어 있으면 동작 안 함
        String certificateDir       // PENDING 재학증명서를 저장할 폴더 (로컬 저장 구현에서 사용)
) {
}

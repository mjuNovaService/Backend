package com.mju.nova.backend.member.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 google.vision.* 설정값. API 키는 환경변수 GOOGLE_VISION_API_KEY로 받는다.
 */
@ConfigurationProperties(prefix = "google.vision")
public record GoogleVisionProperties(String apiKey) {
}

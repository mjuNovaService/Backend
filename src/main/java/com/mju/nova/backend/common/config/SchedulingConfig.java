package com.mju.nova.backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Scheduled 메서드를 정해진 시각에 다음과 같은 기능들이 자동으로 실행됨. (Refresh Token 정리, 대여 알림 등 모든 파트 공통)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}

package com.mju.nova.backend.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 서버 전체가 쓰는 "지금 시각". LocalDateTime.now() 대신 Clock을 주입받아 쓰면 테스트에서 시각을 고정할 수 있다.
 * 대여(반납 기한 18:00, 스케줄러), 학기 자동 전환, 사물함 신청 기간이 모두 한국 시각 기준이라 Asia/Seoul로 고정한다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}

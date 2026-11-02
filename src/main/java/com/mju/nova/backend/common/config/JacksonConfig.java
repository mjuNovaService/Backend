package com.mju.nova.backend.common.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 날짜·시간 형식 전역 설정 (통합 명세 v8: 일시는 yyyy-MM-dd'T'HH:mm:ss).
 * 이 설정이 없으면 LocalDateTime이 "2026-11-15T23:38:17.261988"처럼 소수 초까지 나감
 * 소수 초까지 설정하면 프론트랑 연결 시 오류가 날 수 있어서 소수 초는 버림.
 * Api 모양에서만 소수 초를 버리는 것이지 DB는 소수 초까지 그대로 기록함.
 * 모든 응답의 LocalDateTime에 적용되므로 각 DTO에 @JsonFormat을 따로 붙이지 않아도 됨.
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String TIME_PATTERN = "HH:mm";

    @Bean
    public JsonMapperBuilderCustomizer dateTimeFormatCustomizer() {
        return builder -> builder
                .withConfigOverride(LocalDateTime.class,
                        override -> override.setFormat(JsonFormat.Value.forPattern(DATE_TIME_PATTERN)))
                .withConfigOverride(LocalTime.class,
                        override -> override.setFormat(JsonFormat.Value.forPattern(TIME_PATTERN)));
    }
}

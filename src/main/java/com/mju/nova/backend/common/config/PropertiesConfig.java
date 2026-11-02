package com.mju.nova.backend.common.config;

import com.mju.nova.backend.member.application.EmailVerificationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * application.yaml 값을 담는 @ConfigurationProperties 클래스를 등록하는 곳.
 * 각 파트에서 설정 클래스를 만들면 여기에 추가한다. (JwtProperties는 SecurityConfig에 등록되어 있음)
 */
@Configuration
@EnableConfigurationProperties({
        EmailVerificationProperties.class
})
public class PropertiesConfig {
}

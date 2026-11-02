package com.mju.nova.backend.common.config;

import com.mju.nova.backend.common.security.CustomAccessDeniedHandler;
import com.mju.nova.backend.common.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // JWT를 쓰는 REST API라 세션·폼 로그인·CSRF를 사용 X
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 공개: 회원가입·로그인·재발급·로그아웃
                        .requestMatchers("/api/auth/**").permitAll()
                        // 공개: 서버 상태 확인, 에러 응답, Swagger
                        .requestMatchers("/health", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // TODO(로그인 파트): JWT 필터를 넣는 PR에서 authenticated()로 바꾸고,
                        //  /api/admin/** 는 관리자만 허용. 그 전까지 다른 파트 개발이 막히지 않게 임시로 열기.
                        .anyRequest().permitAll())

                // 401·403도 ErrorResponse 형식으로 응답
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    // 회원가입 때 비밀번호 해시, 로그인 때 비교에 사용
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

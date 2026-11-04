package com.mju.nova.backend.common.config;

import com.mju.nova.backend.common.security.CustomAccessDeniedHandler;
import com.mju.nova.backend.common.security.CustomAuthenticationEntryPoint;
import com.mju.nova.backend.common.security.JwtAuthenticationFilter;
import com.mju.nova.backend.common.security.JwtProperties;
import com.mju.nova.backend.common.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtProvider jwtProvider;

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
                        // 인증 필요: 회원 정보 (Access Token 필수)
                        .requestMatchers("/api/members/**").authenticated()
                        // 학생회(C): 관리자 API는 국장·회장단만, Q&A는 로그인한 학생만
                        // (학생회 소개·링크 조회 GET /api/infos, /api/links 는 공개라 아래 anyRequest에 걸림)
                        // 회원(A): 운영진 권한 변경은 회장단만. 위에서부터 먼저 맞는 규칙이 적용돼서 /api/admin/** 보다 위에 둠
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/members/*/role").hasRole("PRESIDENT")
                        .requestMatchers("/api/admin/**").hasAnyRole("DIRECTOR", "PRESIDENT")
                        .requestMatchers("/api/questions/**").authenticated()
                        // 대여(B): 수령 시간 목록은 비로그인 공개 (대여 신청 화면에서 고르는 목록)
                        .requestMatchers(HttpMethod.GET, "/api/slots").permitAll()
                        // 대여(B): 대여 가능 수량 조회는 비로그인 공개. 아래 /api/rentals/** 보다 먼저 와야 적용된다
                        .requestMatchers(HttpMethod.GET, "/api/rentals/availability").permitAll()
                        // 대여(B): 대여 불가 이벤트 기록은 비로그인 공개 (KPI 수집). 계정 정보는 저장하지 않음
                        .requestMatchers(HttpMethod.POST, "/api/rentals/unavailable-events").permitAll()
                        // 대여(B): 학생용 대여 API는 로그인 필요 (관리자용 /api/admin/rentals 등은 위 /api/admin/** 규칙)
                        .requestMatchers("/api/rentals/**").authenticated()
                        // 사물함(B): 사물함 현황(그리드)은 비로그인 공개. 아래 /api/lockers/** 보다 먼저 와야 적용된다
                        .requestMatchers(HttpMethod.GET, "/api/lockers").permitAll()
                        // 사물함(B): 신청·내 사물함·비밀번호 수정·반납은 로그인 필요
                        .requestMatchers("/api/lockers/**").authenticated()
                        // 그 외는 지금 공개. 각 파트가 로그인이 필요한 경로를 위에 authenticated()로 추가함.
                        // (예: 대여 파트는 rental 구현 때 /api/rentals/** 추가 — 통합 명세 v8 12번 시트)
                        .anyRequest().permitAll())

                // 401·403도 ErrorResponse 형식으로 응답
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // 모든 요청에서 Access Token을 먼저 확인하도록 JWT 필터 추가
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 회원가입 때 비밀번호 해시, 로그인 때 비교에 사용
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

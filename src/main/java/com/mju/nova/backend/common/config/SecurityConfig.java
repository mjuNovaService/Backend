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
                        .requestMatchers("/api/admin/**").hasAnyRole("DIRECTOR", "PRESIDENT")
                        .requestMatchers("/api/questions/**").authenticated()
                        // TODO(로그인 파트): 다른 파트 API도 로그인이 필요해지면 authenticated()로 바꾸고,
                        //  /api/admin/** 는 관리자만 허용. 회원가입 API가 생기기 전까지 다른 파트 개발이 막히지 않게 임시로 열기.
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

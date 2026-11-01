package com.mju.nova.backend.common.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청이 Controller에 가기 전에 거치는 필터.
 * Authorization: Bearer {accessToken} 이 있으면 토큰을 확인하고 "이 요청을 보낸 사람"을 등록한다.
 *
 * 토큰이 없거나 틀려도 여기서 막지 않고 그냥 통과시킨다.
 * 막을지 말지는 SecurityConfig의 URL 규칙이 정한다 (인증 필요 URL이면 EntryPoint가 401 A101).
 *
 * @Component를 붙이지 않는 이유: 붙이면 Spring Boot가 일반 서블릿 필터로도 한 번 더 등록해서
 * 요청마다 두 번 실행된다. 그래서 SecurityConfig에서 직접 만들어 Security 필터 체인에만 넣는다.
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            try {
                // 1. 서명·만료·type="access" 확인 → 학번, role 꺼내기
                LoginStudent loginStudent = jwtProvider.parseAccessToken(token);

                // 2. Spring Security가 이해하는 "인증된 사용자" 객체로 만들기 (role → ROLE_STUDENT 등)
                UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                        loginStudent, null, List.of(new SimpleGrantedAuthority("ROLE_" + loginStudent.role())));

                // 3. 이번 요청의 SecurityContext에 등록 → Controller에서 @AuthenticationPrincipal로 꺼냄
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (JwtException | IllegalArgumentException e) {
                // 만료·위조·형식 오류: 등록하지 않고 통과 → 인증 필요한 URL이면 401
                log.debug("유효하지 않은 Access Token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    // "Authorization: Bearer eyJ..." 에서 "eyJ..." 부분만 꺼낸다
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

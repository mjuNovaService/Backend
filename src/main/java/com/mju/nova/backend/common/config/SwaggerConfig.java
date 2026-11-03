package com.mju.nova.backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(OpenAPI) 설정. 화면 오른쪽 위에 Authorize 버튼을 만들어 Access Token을 넣을 수 있게 한다.
 *
 * 사용법
 * 1. POST /api/auth/login 실행 → 응답의 accessToken 값 복사
 * 2. Authorize 버튼 → 값만 붙여넣기 ("Bearer " 는 Swagger가 붙여줌)
 * 3. 이후 모든 요청에 Authorization: Bearer {토큰} 헤더가 자동으로 붙는다
 *
 * 공개 API(로그인, 소개 글 조회 등)에도 자물쇠 아이콘이 보이지만, 토큰이 없으면 헤더를 안 보낼 뿐 그대로 호출된다.
 * Refresh Token은 쿠키라 브라우저가 알아서 보낸다 (재발급·로그아웃은 로그인 후 바로 호출 가능).
 */
@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME = "JWT";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NOVA 단과대 학생회 API")
                        .version("v1")
                        .description("인공지능·소프트웨어융합대학 학생회 서비스 백엔드 API"))
                // Authorize 버튼에 나오는 입력 방식: HTTP Bearer (JWT)
                .components(new Components().addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("로그인 응답의 accessToken 값만 붙여넣기 (Bearer 없이)")))
                // 모든 API에 위 방식을 적용 → Authorize에 넣은 토큰이 요청마다 자동으로 붙는다
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME));
    }
}

package com.mju.nova.backend.member.presentation;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.member.application.AuthCommandService;
import com.mju.nova.backend.member.application.MemberCommandService;
import com.mju.nova.backend.member.application.dto.LoginResult;
import com.mju.nova.backend.member.application.dto.ReissueResult;
import com.mju.nova.backend.member.application.dto.SignupResponse;
import com.mju.nova.backend.member.presentation.dto.LoginRequest;
import com.mju.nova.backend.member.presentation.dto.LoginResponse;
import com.mju.nova.backend.member.presentation.dto.ReissueResponse;
import com.mju.nova.backend.member.presentation.dto.SignupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "인증", description = "회원가입·로그인·토큰")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandService authCommandService;
    private final MemberCommandService memberCommandService;
    private final RefreshTokenCookieFactory cookieFactory;

    @Operation(summary = "회원가입",
            description = "이메일 인증 후 30분 안에 가입한다. multipart/form-data로 request(JSON, Content-Type: application/json)와 "
                    + "certificate(재학증명서 PDF·JPG·PNG, 5MB 이하)를 보낸다. "
                    + "재학증명서에서 대학·이름이 확인되면 APPROVED, 아니면 PENDING(관리자 확인 대기).")
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            // request 칸: JSON → SignupRequest로 바꾼 뒤 @Valid 검사
            @RequestPart("request") @Valid SignupRequest request,
            // certificate 칸: 업로드한 파일
            @RequestPart("certificate") MultipartFile certificate) {
        SignupResponse response = memberCommandService.signup(request.toCommand(), certificate);
        // 새 회원이 만들어졌으므로 200이 아니라 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "로그인",
            description = "학번과 비밀번호로 로그인한다. Access Token은 Body로, Refresh Token은 HttpOnly 쿠키로 발급한다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        // 1. Service에 로그인 맡기기
        LoginResult result = authCommandService.login(request.studentId(), request.password());

        // 2. Refresh Token은 쿠키로, 나머지는 Body로 나눠 담기
        ResponseCookie cookie = cookieFactory.create(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(LoginResponse.from(result)));
    }

    @Operation(summary = "토큰 재발급",
            description = "쿠키의 Refresh Token으로 새 Access Token과 새 Refresh Token을 발급. 옛 Refresh Token은 더 이상 사용 불가.")
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(
            // 브라우저가 자동으로 보낸 쿠키에서 refreshToken을 꺼냄. 없으면 null
            @Parameter(hidden = true)
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);   // 토큰 없음
        }

        ReissueResult result = authCommandService.reissue(refreshToken);

        ResponseCookie cookie = cookieFactory.create(result.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(ReissueResponse.from(result)));
    }

    @Operation(summary = "로그아웃",
            description = "저장된 Refresh Token을 지우고 쿠키를 삭제. 토큰이 없거나 이미 로그아웃이어도 200.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true)
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authCommandService.logout(refreshToken);
        }

        // 같은 이름·같은 Path, Max-Age=0 → 브라우저가 쿠키 삭제
        ResponseCookie cookie = cookieFactory.delete();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success());
    }
}

package com.mju.nova.backend.member.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.security.LoginStudent;
import com.mju.nova.backend.member.application.MemberQueryService;
import com.mju.nova.backend.member.application.dto.MyInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원", description = "회원 정보")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberQueryService memberQueryService;

    @Operation(summary = "내 정보 조회", description = "로그인한 학생의 학번·이름·이메일을 조회. (마이페이지)")
    @GetMapping("/me")
    public ApiResponse<MyInfoResponse> getMyInfo(
            // 인증 필터가 등록한 "이 요청을 보낸 사람". 요청 값이 아니라 서명된 토큰에서 나온 값이다
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent) {
        return ApiResponse.success(memberQueryService.getMyInfo(loginStudent.studentId()));
    }
}

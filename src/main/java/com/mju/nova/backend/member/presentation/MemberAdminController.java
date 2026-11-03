package com.mju.nova.backend.member.presentation;

import com.mju.nova.backend.common.response.ApiResponse;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.common.security.LoginStudent;
import com.mju.nova.backend.member.application.MemberAdminCommandService;
import com.mju.nova.backend.member.application.MemberAdminQueryService;
import com.mju.nova.backend.member.application.dto.AdminMemberSummaryResponse;
import com.mju.nova.backend.member.application.dto.CertificateFile;
import com.mju.nova.backend.member.application.dto.MemberApproveResponse;
import com.mju.nova.backend.member.domain.ApprovalStatus;
import com.mju.nova.backend.member.presentation.dto.MemberApproveRequest;
import com.mju.nova.backend.member.presentation.dto.MemberRejectRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// /api/admin/** 는 SecurityConfig에서 DIRECTOR·PRESIDENT만 통과
@Tag(name = "회원 (관리자)", description = "가입 승인 대기 회원 확인·승인·반려")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class MemberAdminController {

    private final MemberAdminQueryService memberAdminQueryService;
    private final MemberAdminCommandService memberAdminCommandService;

    @Operation(summary = "회원 목록",
            description = "status 생략=전체, PENDING=승인 대기, APPROVED=승인 완료. 가입 신청이 오래된 순, size 최대 50. 탈퇴 회원 제외.")
    @GetMapping
    public ApiResponse<PageResponse<AdminMemberSummaryResponse>> getMembers(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(memberAdminQueryService.getMembers(status, page, size));
    }

    @Operation(summary = "재학증명서 보기",
            description = "승인 대기 회원이 올린 재학증명서 파일(PDF·JPG·PNG)을 JSON이 아닌 파일 그대로 응답. 보관된 파일이 없으면 404 M007.")
    @GetMapping("/{studentId}/certificate")
    public ResponseEntity<Resource> getCertificate(@PathVariable String studentId) {
        CertificateFile file = memberAdminQueryService.getCertificate(studentId);
        return ResponseEntity.ok()
                .contentType(file.mediaType())
                // inline: 다운로드하지 않고 브라우저 안에서 바로 열기
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
                // 개인정보 파일이라 브라우저·중간 서버에 저장(캐시)하지 않게
                .cacheControl(CacheControl.noStore())
                .body(file.resource());
    }

    @Operation(summary = "승인",
            description = "승인 대기 회원을 승인하고 안내 메일을 보낸다. department는 선택(보내면 그 값으로 저장, 안 보내면 OCR 값 유지). "
                    + "보관한 재학증명서는 삭제. 승인 대기가 아니면 409 M006.")
    @PatchMapping("/{studentId}/approve")
    public ApiResponse<MemberApproveResponse> approve(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable String studentId,
            // Body를 아예 안 보내도 되게 required = false (학과를 바꿀 일이 없으면 빈 요청)
            @RequestBody(required = false) @Valid MemberApproveRequest request) {
        String department = (request == null) ? null : request.department();
        return ApiResponse.success(memberAdminCommandService.approve(loginStudent.studentId(), studentId, department));
    }

    @Operation(summary = "반려",
            description = "반려 사유를 메일로 보내고 회원 정보와 재학증명서를 삭제한다 (같은 학번·이메일로 다시 가입 가능). "
                    + "메일 발송에 실패하면 500 A004, 삭제도 취소. 승인 대기가 아니면 409 M006.")
    @PostMapping("/{studentId}/reject")
    public ApiResponse<Void> reject(
            @Parameter(hidden = true) @AuthenticationPrincipal LoginStudent loginStudent,
            @PathVariable String studentId,
            @RequestBody @Valid MemberRejectRequest request) {
        memberAdminCommandService.reject(loginStudent.studentId(), studentId, request.reason());
        return ApiResponse.success();
    }
}

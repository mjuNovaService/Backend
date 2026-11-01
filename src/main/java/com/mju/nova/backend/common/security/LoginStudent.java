package com.mju.nova.backend.common.security;

/**
 * 인증 필터가 Access Token을 확인한 뒤 "이 요청을 보낸 사람"으로 등록하는 정보.
 * Controller에서 @AuthenticationPrincipal LoginStudent loginStudent 로 꺼내 씀.
 */
public record LoginStudent(String studentId, String role) {
}

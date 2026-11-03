package com.mju.nova.backend.member.domain;

public enum ApprovalStatus {
    PENDING,    // 확인 대기 (재학증명서 인식 실패, 관리자 확인 필요)
    APPROVED    // 승인
    // 반려 상태는 없음: 반려하면 사유를 메일로 보내고 회원 정보를 삭제 (다시 가입 가능)
}

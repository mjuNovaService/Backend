package com.mju.nova.backend.member.domain;

public enum ApprovalStatus {
    PENDING,    // 확인 대기 (재학증명서 인식 실패, 관리자 확인 필요)
    APPROVED,   // 승인
    REJECTED    // 반려
}

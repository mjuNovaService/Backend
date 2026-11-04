package com.mju.nova.backend.locker.domain;

public enum AssignmentStatus {
    IN_USE,     // 사용중
    RETURNED,   // 반납
    CANCELED    // 쓰지 않음 (신청 취소 없음, 명세 2번 시트). 관리자 배정 목록 필터에서도 400 C001
}

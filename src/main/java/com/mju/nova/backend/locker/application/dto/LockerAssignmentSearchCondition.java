package com.mju.nova.backend.locker.application.dto;

import com.mju.nova.backend.locker.domain.AssignmentStatus;

// 관리자 배정 목록 필터. 모두 선택이고 함께 쓰면 AND. semesterId를 생략하면 현재 학기
public record LockerAssignmentSearchCondition(
        Long semesterId,
        AssignmentStatus status,
        String studentId
) {
}

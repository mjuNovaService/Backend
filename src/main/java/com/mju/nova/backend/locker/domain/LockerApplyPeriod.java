package com.mju.nova.backend.locker.domain;

import java.time.LocalDateTime;

/**
 * 사물함 신청 기간 판단. 기간은 현재 학기의 locker_apply_start ~ locker_apply_end (학기 API로 설정).
 * 시작 시각 이상, 종료 시각 미만이면 열림. 둘 중 하나라도 비어 있으면 닫힘.
 */
public final class LockerApplyPeriod {

    private LockerApplyPeriod() {
    }

    public static boolean isOpen(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        return start != null && end != null && !now.isBefore(start) && now.isBefore(end);
    }
}

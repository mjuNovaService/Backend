package com.mju.nova.backend.locker.domain;

/**
 * 사물함 칸 상태. DB 컬럼이 아니라 응답용 계산값 (명세 3번 시트 LockerState).
 * 비활성이 가장 먼저: 지난 학기 미반납이 남은 채 비활성화된 사물함도 DISABLED로 보인다.
 */
public enum LockerState {
    AVAILABLE,  // 빈 칸
    IN_USE,     // 그 학기 사용 중 배정 있음
    DISABLED;   // 사용 불가 (lockerStatus = false)

    public static LockerState of(Locker locker, boolean inUse) {
        if (!locker.isActive()) {
            return DISABLED;
        }
        return inUse ? IN_USE : AVAILABLE;
    }
}

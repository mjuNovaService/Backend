package com.mju.nova.backend.rental.domain;

// 학생이 대여하지 못한 이유 (KPI K4)
public enum UnavailableReason {
    OUT_OF_STOCK,   // 대여 가능한 물품이 없음
    NO_SLOT         // 원하는 수령 시간이 없음
}

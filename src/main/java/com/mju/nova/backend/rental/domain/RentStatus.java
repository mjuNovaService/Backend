package com.mju.nova.backend.rental.domain;

public enum RentStatus {
    RESERVED,   // 예약 (기본값)
    RENTED,     // 대여중
    RETURNED,   // 반납
    CANCELED    // 취소
}

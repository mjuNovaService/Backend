package com.mju.nova.backend.rental.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 대여 불가 이벤트 (KPI K4: 빌리려 했지만 못 빌린 횟수). 화면에서 "대여 불가"를 본 순간 프론트가 남긴다.
 * 누가 남겼는지는 저장하지 않는다 (계정 정보 없음, 비로그인도 기록).
 * 수정하지 않는 기록이라 BaseEntity(updatedAt 포함) 대신 createdAt만 둔다.
 */
@Entity
@Table(indexes = {
        @Index(name = "idx_rental_unavailable_event_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentalUnavailableEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    // 물품 행이 아니라 물품명으로 남긴다 (물품명 단위로 집계)
    @Column(nullable = false, length = 50)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnavailableReason reason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static RentalUnavailableEvent create(String itemName, UnavailableReason reason) {
        RentalUnavailableEvent event = new RentalUnavailableEvent();
        event.itemName = itemName;
        event.reason = reason;
        return event;
    }
}

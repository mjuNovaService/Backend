package com.mju.nova.backend.rental.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.slot.domain.Slot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * "물품당 진행 중(RESERVED·RENTED) 대여 1건만" 규칙은 조건이 붙은 중복 금지라 @Table UK로 만들지 않는다.
 * 코드 검사(+ 락)와 DB 부분 인덱스(uk_rent_item_active)로 처리한다.
 *
 * 상태 전이: (없음) → RESERVED → RENTED(스케줄러) → RETURNED, RESERVED → CANCELED.
 * 상태가 바뀔 때마다 서비스가 RentStatusHistory를 남긴다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rent_id")
    private Long id;

    // 워크인도 슬롯 시간 안에서만 대여
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private LocalDate pickupDate;

    @Column(length = 200)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RentStatus status;

    @Column(nullable = false)
    private LocalDateTime dueAt;

    private LocalDateTime returnedAt;

    // 대여 신청 → 예약 상태로 시작
    public static Rent reserve(Slot slot, Student student, Item item, LocalDate pickupDate,
                               String purpose, LocalDateTime dueAt) {
        Rent rent = new Rent();
        rent.slot = slot;
        rent.student = student;
        rent.item = item;
        rent.pickupDate = pickupDate;
        rent.purpose = purpose;
        rent.dueAt = dueAt;
        rent.status = RentStatus.RESERVED;
        return rent;
    }

    // student는 LAZY지만 getId()는 프록시가 이미 알고 있어 추가 SELECT가 없다
    public boolean isOwnedBy(String studentId) {
        return student.getId().equals(studentId);
    }

    // 수령 시작 시각 = 수령 날짜 + 슬롯 시작 시각
    public LocalDateTime pickupStartAt() {
        return pickupDate.atTime(slot.getStartTime());
    }

    // 연체: 대여 중이면서 반납 기한이 지남 (DB 컬럼 없이 계산)
    public boolean isOverdue(LocalDateTime now) {
        return status == RentStatus.RENTED && now.isAfter(dueAt);
    }

    // 취소 버튼: 예약 상태이고 수령 시작 전
    public boolean isCancelable(LocalDateTime now) {
        return status == RentStatus.RESERVED && now.isBefore(pickupStartAt());
    }

    // 반납완료 버튼: 대여 중 (기한이 지나도 가능)
    public boolean isReturnable() {
        return status == RentStatus.RENTED;
    }

    public void cancel(LocalDateTime now) {
        if (!isCancelable(now)) {
            throw new BusinessException(ErrorCode.RENTAL_NOT_CANCELABLE);
        }
        this.status = RentStatus.CANCELED;
    }

    public void returnItem(LocalDateTime now) {
        if (!isReturnable()) {
            throw new BusinessException(ErrorCode.RENTAL_NOT_RETURNABLE);
        }
        this.status = RentStatus.RETURNED;
        this.returnedAt = now;
    }

    // 기한이 지난 뒤 반납했는지
    public boolean isReturnedLate() {
        return returnedAt != null && returnedAt.isAfter(dueAt);
    }

    /**
     * 스케줄러(SCH-01): 수령 시작 시각이 지난 예약을 대여 중으로 바꾼다.
     * 그 사이 취소됐거나 아직 시작 전이면 바꾸지 않고 false (중복 실행해도 안전).
     */
    public boolean startRental(LocalDateTime now) {
        if (status != RentStatus.RESERVED || now.isBefore(pickupStartAt())) {
            return false;
        }
        this.status = RentStatus.RENTED;
        return true;
    }
}

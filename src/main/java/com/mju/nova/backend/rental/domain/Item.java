package com.mju.nova.backend.rental.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_item_name_item_number", columnNames = {"name", "item_number"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    // 같은 물품명 안에서의 관리 번호
    @Column(nullable = false)
    private Integer itemNumber;

    // 진행 중(RESERVED·RENTED) 대여가 걸려 있으면 false
    @Column(nullable = false)
    private Boolean isAvailable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    // 대여 일수. 충전기·보조배터리 0(당일 반납), 우산 1
    @Column(nullable = false)
    private Integer rentalPeriodDay;

    // 새 물품은 정상 상태, 대여 가능
    public static Item create(String name, Integer itemNumber, Integer rentalPeriodDay) {
        Item item = new Item();
        item.name = name;
        item.itemNumber = itemNumber;
        item.rentalPeriodDay = rentalPeriodDay;
        item.status = ItemStatus.NORMAL;
        item.isAvailable = true;
        return item;
    }

    // 대여 신청 대상이 되는 물품: 정상이고 진행 중 대여가 없음
    public boolean isRentable() {
        return status == ItemStatus.NORMAL && Boolean.TRUE.equals(isAvailable);
    }

    // 대여 신청 → 진행 중 대여가 걸림
    public void occupy() {
        this.isAvailable = false;
    }

    // 예약 취소·반납 → 다시 빌려줄 수 있음
    public void release() {
        this.isAvailable = true;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeItemNumber(Integer itemNumber) {
        this.itemNumber = itemNumber;
    }

    // 같은 물품명 전체에 같은 값을 넣는 건 서비스가 맡는다
    public void changeRentalPeriodDay(Integer rentalPeriodDay) {
        this.rentalPeriodDay = rentalPeriodDay;
    }

    // 점검·미반납 처리. is_available은 건드리지 않는다 (DAMAGED·LOST는 status로 대여 대상에서 빠짐)
    public void changeStatus(ItemStatus status) {
        this.status = status;
    }
}

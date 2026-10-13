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

    @Column(nullable = false)
    private Boolean isAvailable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status;

    // 대여 일수. 충전기·보조배터리 0(당일 반납), 우산 1
    @Column(nullable = false)
    private Integer rentalPeriodDay;
}

package com.mju.nova.backend.locker.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        @UniqueConstraint(name = "uk_locker_locker_number", columnNames = {"locker_number"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Locker extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "locker_id")
    private Long id;

    // 학생에게 보여지는 사물함 번호. 예: 137, A-12
    @Column(nullable = false, length = 20)
    private String lockerNumber;

    // true: 사용 가능, false: 비활성(고장·사용 중지)
    @Column(nullable = false)
    private Boolean lockerStatus;
}

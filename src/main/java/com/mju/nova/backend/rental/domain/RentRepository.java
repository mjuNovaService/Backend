package com.mju.nova.backend.rental.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentRepository extends JpaRepository<Rent, Long> {

    // 진행 중(RESERVED·RENTED) 대여 상태
    List<RentStatus> ACTIVE_STATUSES = List.of(RentStatus.RESERVED, RentStatus.RENTED);

    // 물품 상태 변경 전 확인: 예약이 걸린 물품인가
    boolean existsByItem_IdAndStatus(Long itemId, RentStatus status);

    // 관리자 물품 목록의 currentRent. 물품마다 따로 조회하면 N+1이라 한 번에 가져온다
    @EntityGraph(attributePaths = "student")
    List<Rent> findByItem_IdInAndStatusIn(Collection<Long> itemIds, Collection<RentStatus> statuses);
}

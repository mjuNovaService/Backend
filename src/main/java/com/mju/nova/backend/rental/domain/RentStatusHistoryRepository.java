package com.mju.nova.backend.rental.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentStatusHistoryRepository extends JpaRepository<RentStatusHistory, Long> {

    // 시간순. 같은 시각이면 먼저 쌓인 이력이 위로
    List<RentStatusHistory> findByRent_IdOrderByCreatedAtAscIdAsc(Long rentId);

    // 여러 대여의 이력을 한 번에 (CSV의 전이 시각용. 대여마다 조회하면 N+1)
    List<RentStatusHistory> findByRent_IdInOrderByCreatedAtAscIdAsc(Collection<Long> rentIds);
}

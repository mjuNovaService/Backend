package com.mju.nova.backend.rental.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentStatusHistoryRepository extends JpaRepository<RentStatusHistory, Long> {

    // 시간순. 같은 시각이면 먼저 쌓인 이력이 위로
    List<RentStatusHistory> findByRent_IdOrderByCreatedAtAscIdAsc(Long rentId);
}

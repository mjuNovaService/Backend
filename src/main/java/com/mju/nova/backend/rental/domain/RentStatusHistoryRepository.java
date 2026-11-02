package com.mju.nova.backend.rental.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RentStatusHistoryRepository extends JpaRepository<RentStatusHistory, Long> {
}

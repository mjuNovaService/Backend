package com.mju.nova.backend.locker.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LockerPasswordAccessLogRepository extends JpaRepository<LockerPasswordAccessLog, Long> {
}

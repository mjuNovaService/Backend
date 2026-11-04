package com.mju.nova.backend.locker.domain;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LockerRepository extends JpaRepository<Locker, Long> {

    // 등록 순서 = 목록 순서 (명세: lockerId 오름차순)
    List<Locker> findAllByOrderByIdAsc();

    // 사물함 등록 (LC009): 보낸 번호 중 이미 등록된 게 하나라도 있나
    boolean existsByLockerNumberIn(Collection<String> lockerNumbers);

    // 선착순 신청·비활성화가 같은 사물함을 동시에 다루지 않도록 사물함 행을 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Locker l where l.id = :id")
    Optional<Locker> findByIdForUpdate(@Param("id") Long id);
}

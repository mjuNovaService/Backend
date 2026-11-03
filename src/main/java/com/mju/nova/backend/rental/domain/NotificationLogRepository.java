package com.mju.nova.backend.rental.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>,
        JpaSpecificationExecutor<NotificationLog> {

    boolean existsByRent_IdAndType(Long rentId, NotificationType type);

    // NOTI-RETRY: 실패한 알림 중 대여가 아직 끝나지 않은 것만 (반납된 대여에 다시 보내지 않음)
    @Query("select n.id from NotificationLog n where n.status = :sendStatus and n.rent.status = :rentStatus")
    List<Long> findIdsToRetry(@Param("sendStatus") SendStatus sendStatus, @Param("rentStatus") RentStatus rentStatus);

    @EntityGraph(attributePaths = {"rent", "rent.student", "rent.item"})
    @Query("select n from NotificationLog n where n.id = :id")
    Optional<NotificationLog> findWithRentById(@Param("id") Long id);

    // 관리자 알림 기록. 학생·물품 정보를 한 번에 가져와 N+1을 막는다 (count 쿼리에는 적용되지 않음)
    @Override
    @EntityGraph(attributePaths = {"rent", "rent.student", "rent.item"})
    Page<NotificationLog> findAll(Specification<NotificationLog> spec, Pageable pageable);
}

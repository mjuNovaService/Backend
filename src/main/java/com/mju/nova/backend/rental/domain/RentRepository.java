package com.mju.nova.backend.rental.domain;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentRepository extends JpaRepository<Rent, Long>, JpaSpecificationExecutor<Rent> {

    // 진행 중(RESERVED·RENTED) 대여 상태
    List<RentStatus> ACTIVE_STATUSES = List.of(RentStatus.RESERVED, RentStatus.RENTED);

    // 1인 1개: 그 학기 슬롯의 진행 중 대여가 있는가 (대여의 학기 = slot.semester)
    boolean existsByStudent_IdAndSlot_Semester_IdAndStatusIn(String studentId, Long semesterId,
                                                            Collection<RentStatus> statuses);

    // 물품 상태 변경 전 확인: 예약이 걸린 물품인가
    boolean existsByItem_IdAndStatus(Long itemId, RentStatus status);

    // 슬롯 시각 변경 전 확인: 예약이 걸린 슬롯인가
    boolean existsBySlot_IdAndStatus(Long slotId, RentStatus status);

    // 관리자 물품 목록의 currentRent. 물품마다 따로 조회하면 N+1이라 한 번에 가져온다
    @EntityGraph(attributePaths = "student")
    List<Rent> findByItem_IdInAndStatusIn(Collection<Long> itemIds, Collection<RentStatus> statuses);

    // 내 대여 목록. 물품명·슬롯 시각을 같이 보여줘야 해서 item·slot을 함께 가져온다
    @EntityGraph(attributePaths = {"item", "slot"})
    List<Rent> findByStudent_Id(String studentId, Sort sort);

    @EntityGraph(attributePaths = {"item", "slot"})
    List<Rent> findByStudent_IdAndStatus(String studentId, RentStatus status, Sort sort);

    @EntityGraph(attributePaths = {"item", "slot"})
    @Query("select r from Rent r where r.id = :id")
    Optional<Rent> findWithItemAndSlotById(@Param("id") Long id);

    // 취소·반납·스케줄러가 같은 대여를 동시에 바꾸지 않도록 잠그고 가져온다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rent r where r.id = :id")
    Optional<Rent> findByIdForUpdate(@Param("id") Long id);

    // 알림 메일에 학생 이메일·물품명이 필요하다
    @EntityGraph(attributePaths = {"student", "item"})
    @Query("select r from Rent r where r.id = :id")
    Optional<Rent> findWithStudentAndItemById(@Param("id") Long id);

    // SCH-01: 수령 시작 시각이 지난 예약. 지난 날짜에 남은 예약도 같이 (서버가 꺼져 있어 놓친 경우)
    @Query("""
            select r.id from Rent r join r.slot s
            where r.status = :status
              and (r.pickupDate < :today or (r.pickupDate = :today and s.startTime <= :time))
            """)
    List<Long> findIdsToStart(@Param("status") RentStatus status, @Param("today") LocalDate today,
                              @Param("time") LocalTime time);

    // NOTI-01: 반납 기한이 정확히 그 시각인 대여 중 아직 그 종류 알림을 안 보낸 것
    @Query("""
            select r.id from Rent r
            where r.status = :status and r.dueAt = :dueAt
              and not exists (select n from NotificationLog n where n.rent = r and n.type = :type)
            """)
    List<Long> findIdsDueAtWithoutNotification(@Param("status") RentStatus status,
                                               @Param("dueAt") LocalDateTime dueAt,
                                               @Param("type") NotificationType type);

    // NOTI-02: 반납 기한이 지난 대여 중 아직 그 종류 알림을 안 보낸 것
    @Query("""
            select r.id from Rent r
            where r.status = :status and r.dueAt < :now
              and not exists (select n from NotificationLog n where n.rent = r and n.type = :type)
            """)
    List<Long> findIdsOverdueWithoutNotification(@Param("status") RentStatus status,
                                                 @Param("now") LocalDateTime now,
                                                 @Param("type") NotificationType type);

    // 관리자 대여 목록. 학생·물품·슬롯·담당자를 한 번에 가져와 N+1을 막는다 (count 쿼리에는 적용되지 않음)
    @Override
    @EntityGraph(attributePaths = {"student", "item", "slot", "slot.manager"})
    Page<Rent> findAll(Specification<Rent> spec, Pageable pageable);

    // 대여 이력 CSV. 페이지 없이 전체, 학생·물품·슬롯을 한 번에 가져온다
    @Override
    @EntityGraph(attributePaths = {"student", "item", "slot"})
    List<Rent> findAll(Specification<Rent> spec, Sort sort);
}

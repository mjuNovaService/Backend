package com.mju.nova.backend.slot.domain;

import jakarta.persistence.LockModeType;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 정렬은 하지 않는다. day_of_week가 문자열로 저장돼 DB 정렬은 FRI·MON·THU… 순이 되므로 서비스에서 요일 순으로 정렬한다.
 */
public interface SlotRepository extends JpaRepository<Slot, Long> {

    boolean existsBySemester_Id(Long semesterId);

    // 같은 학기·요일·시작 시각 중복 확인 (uk_slot_semester_id_day_of_week_start_time)
    boolean existsBySemester_IdAndDayOfWeekAndStartTime(Long semesterId, DayOfWeek dayOfWeek, LocalTime startTime);

    boolean existsBySemester_IdAndDayOfWeekAndStartTimeAndIdNot(Long semesterId, DayOfWeek dayOfWeek,
                                                               LocalTime startTime, Long id);

    // 학생용: 열린 슬롯만
    List<Slot> findBySemester_IdAndStatusTrue(Long semesterId);

    // 관리자용: 담당자 이름을 같이 보여줘야 해서 manager를 함께 가져온다
    @EntityGraph(attributePaths = "manager")
    List<Slot> findBySemester_Id(Long semesterId);

    // 관리자 수정: 같은 슬롯에 대여 신청이 들어오는 중이면 끝날 때까지 기다린다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") Long id);

    // 대여 신청: 공유 잠금 (신청끼리는 서로 막지 않고, 관리자 수정과만 순서를 맞춘다)
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findByIdForShare(@Param("id") Long id);
}

package com.mju.nova.backend.locker.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LockerAssignmentRepository extends JpaRepository<LockerAssignment, Long>,
        JpaSpecificationExecutor<LockerAssignment> {

    // 1인 1개 (LC005): 이 학생이 이 학기에 사용 중인 배정이 있나. 지난 학기 미반납은 보지 않는다
    boolean existsByStudent_IdAndSemester_IdAndStatus(String studentId, Long semesterId, AssignmentStatus status);

    // 사물함당 1명 (LC004·LC010): 이 사물함에 이 학기 사용 중인 배정이 있나
    boolean existsByLocker_IdAndSemester_IdAndStatus(Long lockerId, Long semesterId, AssignmentStatus status);

    // 사물함 현황의 state 계산. 사물함마다 따로 조회하지 않고 사용 중인 사물함 ID만 한 번에 가져온다
    @Query("""
            select a.locker.id from LockerAssignment a
            where a.semester.id = :semesterId and a.status = :status
            """)
    List<Long> findLockerIds(@Param("semesterId") Long semesterId, @Param("status") AssignmentStatus status);

    // 관리자 사물함 목록의 assignment. 사물함마다 따로 조회하면 N+1이라 학생까지 한 번에 가져온다
    @EntityGraph(attributePaths = "student")
    List<LockerAssignment> findBySemester_IdAndStatus(Long semesterId, AssignmentStatus status);

    // 내 사물함. 학기 이름·사물함 번호를 같이 보여줘야 해서 semester·locker를 함께 가져온다
    @EntityGraph(attributePaths = {"semester", "locker"})
    List<LockerAssignment> findByStudent_IdOrderByIdDesc(String studentId);

    // 반납·비밀번호 수정이 같은 배정을 동시에 바꾸지 않도록 잠그고 가져온다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LockerAssignment a where a.id = :id")
    Optional<LockerAssignment> findByIdForUpdate(@Param("id") Long id);

    // 관리자 배정 목록. 학기·사물함·학생을 한 번에 가져와 N+1을 막는다 (count 쿼리에는 적용되지 않음)
    @Override
    @EntityGraph(attributePaths = {"semester", "locker", "student"})
    Page<LockerAssignment> findAll(Specification<LockerAssignment> spec, Pageable pageable);
}

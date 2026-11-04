package com.mju.nova.backend.locker.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LockerPasswordAccessLogRepository extends JpaRepository<LockerPasswordAccessLog, Long> {

    // 열람 기록 목록. 사물함 번호·배정 학생·열람자를 한 번에 가져와 N+1을 막는다 (count 쿼리에는 적용되지 않음)
    @Override
    @EntityGraph(attributePaths = {"assignment", "assignment.locker", "assignment.student", "viewer"})
    Page<LockerPasswordAccessLog> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"assignment", "assignment.locker", "assignment.student", "viewer"})
    Page<LockerPasswordAccessLog> findByAssignment_Id(Long assignmentId, Pageable pageable);
}

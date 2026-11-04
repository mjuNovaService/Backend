package com.mju.nova.backend.semester.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    // 현재 학기 (is_current = true). 전체에서 1개
    Optional<Semester> findByIsCurrentTrue();

    Optional<Semester> findByName(String name);

    // 관리자 목록: 최근 학기가 위로
    List<Semester> findAllByOrderByStartDateDesc();
}

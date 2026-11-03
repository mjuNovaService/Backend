package com.mju.nova.backend.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

// PK가 학번(String)이라 두 번째 타입이 String
public interface StudentRepository extends JpaRepository<Student, String> {

    boolean existsByEmail(String email);

    // 이 역할의 회원이 한 명이라도 있나 (최초 관리자 확인용)
    boolean existsByRole(StudentRole role);
}

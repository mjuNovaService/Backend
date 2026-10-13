package com.mju.nova.backend.member.domain;

import org.springframework.data.jpa.repository.JpaRepository;

// PK가 학번(String)이라 두 번째 타입이 String
public interface StudentRepository extends JpaRepository<Student, String> {
}

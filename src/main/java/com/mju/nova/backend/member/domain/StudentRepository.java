package com.mju.nova.backend.member.domain;

import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// PK가 학번(String)이라 두 번째 타입이 String
public interface StudentRepository extends JpaRepository<Student, String> {

    boolean existsByEmail(String email);

    // 이 역할의 회원이 한 명이라도 있나 (최초 관리자 확인용)
    boolean existsByRole(StudentRole role);

    // 관리자 회원 목록 (탈퇴 회원 제외). 정렬·페이지는 Pageable로 받음
    Page<Student> findByDeletedAtIsNull(Pageable pageable);

    Page<Student> findByApprovalStatusAndDeletedAtIsNull(ApprovalStatus approvalStatus, Pageable pageable);

    // role 필터 (운영진 목록: role IN (DIRECTOR, PRESIDENT))
    Page<Student> findByRoleInAndDeletedAtIsNull(Collection<StudentRole> roles, Pageable pageable);

    Page<Student> findByApprovalStatusAndRoleInAndDeletedAtIsNull(
            ApprovalStatus approvalStatus, Collection<StudentRole> roles, Pageable pageable);

    // 이 역할의 회원 수 (탈퇴 제외). 마지막 회장 보호용
    long countByRoleAndDeletedAtIsNull(StudentRole role);
}

package com.mju.nova.backend.council.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // 내 질문 목록: WHERE student_id = ? (student.id 를 Student_Id 로 따라감)
    Page<Question> findByStudent_Id(String studentId, Pageable pageable);

    // 관리자 목록은 작성자 이름도 보여줘야 해서 student를 같이 가져온다 (질문마다 SELECT가 따로 나가는 N+1 방지)
    @EntityGraph(attributePaths = "student")
    @Query("select q from Question q")
    Page<Question> findAllWithStudent(Pageable pageable);

    @EntityGraph(attributePaths = "student")
    @Query("select q from Question q where exists (select a from Answer a where a.question = q)")
    Page<Question> findAnsweredWithStudent(Pageable pageable);

    @EntityGraph(attributePaths = "student")
    @Query("select q from Question q where not exists (select a from Answer a where a.question = q)")
    Page<Question> findUnansweredWithStudent(Pageable pageable);

    @EntityGraph(attributePaths = "student")
    @Query("select q from Question q where q.id = :id")
    Optional<Question> findWithStudentById(@Param("id") Long id);
}

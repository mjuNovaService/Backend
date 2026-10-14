package com.mju.nova.backend.council.domain;

import com.mju.nova.backend.common.entity.BaseEntity;
import com.mju.nova.backend.member.domain.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 질문 1건당 답변 1건. Question의 PK를 그대로 PK로 쓴다(PK 공유 1:1).
 * question_id 컬럼 하나가 PK이자 FK라 @GeneratedValue 없음.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseEntity {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    // 답변 작성 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Student admin;

    @Column(nullable = false, columnDefinition = "text")
    private String content;
}

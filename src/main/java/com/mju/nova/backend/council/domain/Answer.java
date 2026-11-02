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

    // id는 넣지 않는다. @MapsId가 저장할 때 question의 id를 그대로 복사해 PK로 쓴다
    public static Answer create(Question question, Student admin, String content) {
        Answer answer = new Answer();
        answer.question = question;
        answer.admin = admin;
        answer.content = content;
        return answer;
    }

    // 답변 수정: 내용과 "마지막으로 답변을 고친 관리자"를 바꾼다. 처음 답변 시각은 created_at에 남는다
    public void update(Student admin, String content) {
        this.admin = admin;
        this.content = content;
    }
}

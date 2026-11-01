package com.mju.nova.backend.council.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.council.application.dto.QuestionIdResponse;
import com.mju.nova.backend.council.domain.AnswerRepository;
import com.mju.nova.backend.council.domain.Question;
import com.mju.nova.backend.council.domain.QuestionRepository;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학생의 질문 등록·삭제 (Command).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QuestionCommandService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final StudentRepository studentRepository;

    public QuestionIdResponse create(String studentId, String title, String content) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Question question = questionRepository.save(Question.create(student, title, content));
        return new QuestionIdResponse(question.getId());
    }

    public void delete(String studentId, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        if (!question.isWrittenBy(studentId)) {
            throw new BusinessException(ErrorCode.QUESTION_ACCESS_DENIED);
        }
        // 답변이 달린 질문을 지우면 answer 행이 FK 때문에 남을 수 없고, 관리자가 쓴 답변도 같이 사라진다 → 막는다
        if (answerRepository.existsById(questionId)) {
            throw new BusinessException(ErrorCode.ANSWERED_QUESTION_NOT_DELETABLE);
        }
        questionRepository.delete(question);
    }
}

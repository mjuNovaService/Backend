package com.mju.nova.backend.council.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.council.application.dto.QuestionIdResponse;
import com.mju.nova.backend.council.domain.Answer;
import com.mju.nova.backend.council.domain.AnswerRepository;
import com.mju.nova.backend.council.domain.Question;
import com.mju.nova.backend.council.domain.QuestionRepository;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자의 답변 등록·수정 (Command). 질문 1개에 답변 1개.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AnswerCommandService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;

    public QuestionIdResponse create(String adminId, Long questionId, String content) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        if (answerRepository.existsById(questionId)) {
            throw new BusinessException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }
        Student admin = findStudent(adminId);
        try {
            // 두 관리자가 동시에 답변해도 answer의 PK(question_id)가 같아서 DB가 두 번째 INSERT를 막는다
            answerRepository.saveAndFlush(Answer.create(question, admin, content));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }
        return new QuestionIdResponse(questionId);
    }

    public QuestionIdResponse update(String adminId, Long questionId, String content) {
        Answer answer = answerRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
        answer.update(findStudent(adminId), content);
        return new QuestionIdResponse(questionId);
    }

    private Student findStudent(String studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}

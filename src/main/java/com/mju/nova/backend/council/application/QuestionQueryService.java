package com.mju.nova.backend.council.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.council.application.dto.AdminQuestionDetailResponse;
import com.mju.nova.backend.council.application.dto.AdminQuestionSummaryResponse;
import com.mju.nova.backend.council.application.dto.MyQuestionDetailResponse;
import com.mju.nova.backend.council.application.dto.MyQuestionSummaryResponse;
import com.mju.nova.backend.common.response.PageRequests;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.council.domain.Answer;
import com.mju.nova.backend.council.domain.AnswerRepository;
import com.mju.nova.backend.council.domain.Question;
import com.mju.nova.backend.council.domain.QuestionRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Q&A 조회 (Query). 학생용(내 질문)과 관리자용(전체 질문)을 같이 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionQueryService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    // ── 학생 ──

    public PageResponse<MyQuestionSummaryResponse> getMyQuestions(String studentId, int page, int size) {
        Page<Question> questions = questionRepository.findByStudent_Id(studentId, latestFirst(page, size));
        Set<Long> answeredIds = answeredIdsOf(questions.getContent());
        return PageResponse.of(questions, q -> new MyQuestionSummaryResponse(
                q.getId(), q.getTitle(), answeredIds.contains(q.getId()), q.getCreatedAt()));
    }

    public MyQuestionDetailResponse getMyQuestion(String studentId, Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        if (!question.isWrittenBy(studentId)) {
            throw new BusinessException(ErrorCode.QUESTION_ACCESS_DENIED);
        }
        // Answer의 PK = questionId 라서 findById(questionId)로 바로 찾는다
        Answer answer = answerRepository.findById(questionId).orElse(null);
        return MyQuestionDetailResponse.of(question, answer);
    }

    // ── 관리자 ──

    // answered: null이면 전체, true면 답변 완료만, false면 답변 대기만
    public PageResponse<AdminQuestionSummaryResponse> getAllQuestions(Boolean answered, int page, int size) {
        Pageable pageable = latestFirst(page, size);
        Page<Question> questions;
        if (answered == null) {
            questions = questionRepository.findAllWithStudent(pageable);
        } else if (answered) {
            questions = questionRepository.findAnsweredWithStudent(pageable);
        } else {
            questions = questionRepository.findUnansweredWithStudent(pageable);
        }
        Set<Long> answeredIds = answeredIdsOf(questions.getContent());
        return PageResponse.of(questions, q -> AdminQuestionSummaryResponse.of(q, answeredIds.contains(q.getId())));
    }

    public AdminQuestionDetailResponse getQuestionForAdmin(Long questionId) {
        Question question = questionRepository.findWithStudentById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        Answer answer = answerRepository.findById(questionId).orElse(null);
        return AdminQuestionDetailResponse.of(question, answer);
    }

    // ── 공통 ──

    // 이번 페이지 질문들 중 답변이 있는 질문 id를 한 번의 SELECT로 모은다 (질문마다 따로 조회하면 N+1)
    private Set<Long> answeredIdsOf(List<Question> questions) {
        List<Long> ids = questions.stream().map(Question::getId).toList();
        return answerRepository.findAllById(ids).stream()
                .map(Answer::getId)
                .collect(Collectors.toSet());
    }

    // 최신 질문이 위로. page·size 보정은 공통 규칙(PageRequests)을 따른다
    private Pageable latestFirst(int page, int size) {
        return PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }
}

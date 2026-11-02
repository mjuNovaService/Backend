package com.mju.nova.backend.council.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.council.application.dto.InfoIdResponse;
import com.mju.nova.backend.council.domain.Info;
import com.mju.nova.backend.council.domain.InfoRepository;
import com.mju.nova.backend.council.domain.InfoType;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학생회 소개 등록·수정·삭제 (Command). DB 상태를 바꿈.
 * 관리자인지는 SecurityConfig(/api/admin/**)에서 이미 걸렀으므로 여기서는 다시 보지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InfoCommandService {

    private final InfoRepository infoRepository;
    private final StudentRepository studentRepository;

    public InfoIdResponse create(String adminId, InfoType type, String title, String content) {
        checkSingleType(type);
        Student admin = findStudent(adminId);
        Info info = infoRepository.save(Info.create(type, title, content, admin));
        return new InfoIdResponse(info.getId());
    }

    public InfoIdResponse update(String adminId, Long infoId, InfoType type, String title, String content) {
        Info info = findInfo(infoId);
        // 다른 종류로 옮길 때만 검사. 같은 종류면 자기 자신이라 괜찮다
        if (type != null && type != info.getType()) {
            checkSingleType(type);
        }
        Student admin = findStudent(adminId);
        // save()를 부르지 않아도 트랜잭션이 끝날 때 바뀐 칸을 찾아 UPDATE (변경 감지, dirty checking)
        info.update(type, title, content, admin);
        return new InfoIdResponse(info.getId());
    }

    public void delete(Long infoId) {
        infoRepository.delete(findInfo(infoId));
    }

    // FAQ를 뺀 종류(INTRO·PLEDGE·ORG_CHART·RULES)는 글이 이미 있으면 새로 못 만든다 → 기존 글을 수정해야 함
    // (관리자 두 명이 동시에 등록하는 경우는 DB 제약이 없어 막지 못한다. 관리자 전용이라 허용)
    private void checkSingleType(InfoType type) {
        if (type.isSingle() && infoRepository.existsByType(type)) {
            throw new BusinessException(ErrorCode.INFO_TYPE_ALREADY_EXISTS);
        }
    }

    private Info findInfo(Long infoId) {
        return infoRepository.findById(infoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INFO_NOT_FOUND));
    }

    private Student findStudent(String studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}

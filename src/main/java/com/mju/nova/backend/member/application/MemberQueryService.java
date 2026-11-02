package com.mju.nova.backend.member.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.application.dto.MyInfoResponse;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 조회 작업 (Query). DB를 읽기만 함.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final StudentRepository studentRepository;

    public MyInfoResponse getMyInfo(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return MyInfoResponse.from(student);
    }
}

package com.mju.nova.backend.member.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.response.PageRequests;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.member.application.dto.AdminMemberSummaryResponse;
import com.mju.nova.backend.member.application.dto.CertificateFile;
import com.mju.nova.backend.member.domain.ApprovalStatus;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.member.domain.StudentRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 조회 (Query). 승인 대기 목록, 재학증명서 보기.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAdminQueryService {

    private final StudentRepository studentRepository;
    private final CertificateStorage certificateStorage;

    // status: null이면 전체, PENDING이면 승인 대기만, APPROVED면 승인 완료만
    // roles: null·빈 목록이면 전체, 있으면 그 역할만 (운영진 목록 = DIRECTOR, PRESIDENT)
    public PageResponse<AdminMemberSummaryResponse> getMembers(ApprovalStatus status, List<StudentRole> roles,
                                                               int page, int size) {
        // 가입 신청이 오래된 순 (먼저 신청한 학생부터 처리). 시각이 같으면 학번순
        Pageable pageable = PageRequests.of(page, size, Sort.by("createdAt").ascending().and(Sort.by("id")));
        boolean allRoles = (roles == null || roles.isEmpty());
        Page<Student> students;
        if (allRoles) {
            students = (status == null)
                    ? studentRepository.findByDeletedAtIsNull(pageable)
                    : studentRepository.findByApprovalStatusAndDeletedAtIsNull(status, pageable);
        } else {
            students = (status == null)
                    ? studentRepository.findByRoleInAndDeletedAtIsNull(roles, pageable)
                    : studentRepository.findByApprovalStatusAndRoleInAndDeletedAtIsNull(status, roles, pageable);
        }
        return PageResponse.of(students, AdminMemberSummaryResponse::from);
    }

    public CertificateFile getCertificate(String studentId) {
        // 1. 회원 조회 (탈퇴 회원은 없는 회원으로)
        Student student = studentRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 2. 보관한 재학증명서가 있나 (자동 승인된 회원, 이미 처리된 회원은 없음)
        String key = student.getCertificateImageKey();
        if (key == null) {
            throw new BusinessException(ErrorCode.CERTIFICATE_NOT_FOUND);
        }
        Resource resource = certificateStorage.load(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.CERTIFICATE_NOT_FOUND));

        // 3. 확장자로 형식 정하기 (.pdf → application/pdf, .png → image/png ...)
        MediaType mediaType = MediaTypeFactory.getMediaType(key).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return new CertificateFile(resource, mediaType);
    }
}

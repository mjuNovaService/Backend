package com.mju.nova.backend.member.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.mail.EmailSendException;
import com.mju.nova.backend.common.mail.EmailSender;
import com.mju.nova.backend.member.application.dto.MemberApproveResponse;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * 관리자 회원 승인·반려.
 *
 * 회원가입처럼 TransactionTemplate을 쓰는 이유:
 * 재학증명서 파일 삭제와 승인 메일은 "DB 저장이 확실히 끝난 뒤"에 해야 한다.
 * (파일을 먼저 지웠는데 DB 저장이 실패하면, 관리자가 다시 확인할 파일이 사라진다)
 * 그래서 DB 작업만 트랜잭션으로 묶고, 끝난 뒤에 파일 삭제·메일을 처리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberAdminCommandService {

    private final StudentRepository studentRepository;
    private final CertificateStorage certificateStorage;
    private final EmailSender emailSender;
    private final TransactionTemplate transactionTemplate;

    public MemberApproveResponse approve(String adminId, String studentId, String department) {
        // 학과를 빈 글자로 보내면 "안 보낸 것"으로 처리
        String newDepartment = StringUtils.hasText(department) ? department.trim() : null;

        // 1~3. DB 작업 (한 트랜잭션)
        Approval approval = transactionTemplate.execute(tx -> {
            Student student = findPendingStudent(studentId);
            String certificateKey = student.getCertificateImageKey();   // approve()가 비우기 전에 꺼내 둠
            student.approve(newDepartment);   // 값만 바꾸면 트랜잭션이 끝날 때 변경 감지로 UPDATE
            return new Approval(student, certificateKey);
        });
        Student student = approval.student();
        log.info("회원 승인: {} (처리: {})", studentId, adminId);

        // 4. 재학증명서 파일 삭제 (DB 저장이 끝난 뒤)
        if (approval.certificateKey() != null) {
            certificateStorage.delete(approval.certificateKey());
        }

        // 5. 승인 안내 메일. 실패해도 승인은 유지 (학생이 로그인해 보면 알 수 있음)
        try {
            emailSender.send(student.getEmail(), "[NOVA] 회원가입이 승인되었습니다", buildApproveBody(student.getName()));
        } catch (EmailSendException e) {
            log.warn("승인 메일 발송 실패 (승인은 완료): {}", studentId, e);
        }

        return MemberApproveResponse.from(student);
    }

    public void reject(String adminId, String studentId, String reason) {
        String trimmedReason = reason.trim();

        // 1~3. DB 작업 + 반려 메일 (한 트랜잭션)
        String certificateKey = transactionTemplate.execute(tx -> {
            Student student = findPendingStudent(studentId);
            String key = student.getCertificateImageKey();

            // 회원 삭제. flush()로 DELETE를 지금 실행 → 삭제가 실패하면 메일을 보내기 전에 멈춤
            studentRepository.delete(student);
            studentRepository.flush();

            // 반려 메일. 실패하면 예외 → 트랜잭션 롤백 → 삭제도 취소 (사유를 모른 채 정보만 사라지는 일 방지)
            try {
                emailSender.send(student.getEmail(), "[NOVA] 회원가입 승인이 반려되었습니다",
                        buildRejectBody(student.getName(), trimmedReason));
            } catch (EmailSendException e) {
                throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
            }
            return key;
        });
        // 반려 사유·이름은 개인정보라 로그에 남기지 않음. 누가 처리했는지만 기록
        log.info("회원 반려: {} (처리: {})", studentId, adminId);

        // 4. 재학증명서 파일 삭제 (DB 삭제가 끝난 뒤)
        if (certificateKey != null) {
            certificateStorage.delete(certificateKey);
        }
    }

    // 승인·반려 공통: 회원이 있고(탈퇴 제외) 승인 대기 중인가
    private Student findPendingStudent(String studentId) {
        Student student = studentRepository.findById(studentId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!student.isPending()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_PENDING);
        }
        return student;
    }

    private String buildApproveBody(String name) {
        return """
                %s님, NOVA 회원가입이 승인되었습니다.

                이제 학번과 비밀번호로 로그인할 수 있습니다.
                """.formatted(name);
    }

    private String buildRejectBody(String name, String reason) {
        return """
                %s님, NOVA 회원가입 승인이 반려되었습니다.

                반려 사유: %s

                가입 정보는 삭제되었습니다. 사유를 확인한 뒤 처음부터 다시 가입해 주세요.
                """.formatted(name, reason);
    }

    // 트랜잭션 안에서 꺼낸 값을 밖으로 전달하는 상자 (승인된 회원 + 지울 파일 위치)
    private record Approval(Student student, String certificateKey) {
    }
}

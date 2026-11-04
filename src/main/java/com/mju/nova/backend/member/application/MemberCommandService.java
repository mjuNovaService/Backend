package com.mju.nova.backend.member.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.member.application.dto.SignupCommand;
import com.mju.nova.backend.member.application.dto.SignupResponse;
import com.mju.nova.backend.member.domain.ApprovalStatus;
import com.mju.nova.backend.member.domain.EmailVerification;
import com.mju.nova.backend.member.domain.EmailVerificationRepository;
import com.mju.nova.backend.member.domain.Student;
import com.mju.nova.backend.member.domain.StudentRepository;
import com.mju.nova.backend.member.domain.StudentRole;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원가입
 *
 * 클래스에 @Transactional을 붙이지 않은 이유:
 * 재학증명서 OCR을 구현할 때 외부 API를 호출하는 과정에서 시간이 지연될 시
 * 다른 외부 API를 호출하는 작업(로그인) 또한 진행하지 못하므로
 * 확인·OCR은 트랜잭션 밖에서 하고, 마지막 저장만 TransactionTemplate으로 묶음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_CERTIFICATE_SIZE = 5L * 1024 * 1024;   // 5MB (yaml의 max-file-size와 같게)

    private final StudentRepository studentRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final CertificateReader certificateReader;
    private final CertificateStorage certificateStorage;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final EmailVerificationProperties emailProperties;
    private final SignupProperties signupProperties;

    public SignupResponse signup(SignupCommand command, MultipartFile certificate) {
        // 1. 입력값 소문자 정리
        String email = normalizeEmail(command.email());
        String name = command.name().trim();

        // 2. 이메일 인증을 마친 기록이 있는지? 확인 (가장 최근 인증 기록 기준)
        EmailVerification verification = emailVerificationRepository.findTopByEmailOrderByIdDesc(email)
                .filter(EmailVerification::isVerified)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));

        // 3. 인증하고 30분 안에 가입하는가
        if (verification.isSignupWindowExpired(emailProperties.signupWindow())) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        // 4. 이미 가입된 학번·이메일인가
        if (studentRepository.existsById(command.studentId())) {
            throw new BusinessException(ErrorCode.STUDENT_ID_ALREADY_REGISTERED);
        }
        if (studentRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        // 5. 재학증명서 파일 확인 (비어 있음·형식·크기)
        validateCertificateFile(certificate);

        // 6. 재학증명서 읽기 (OCR). 키가 없거나 실패하면 빈 Optional
        Optional<CertificateInfo> certificateInfo = certificateReader.read(certificate);
        boolean certificateMatched = certificateInfo.map(info -> matchesCertificate(info, name)).orElse(false);

        // 7. 역할·승인 상태 정하기
        //    최초 관리자(환경변수 이메일 + 아직 회장 없음)는 OCR 결과와 상관없이 회장 + 승인
        boolean initialAdmin = isInitialAdmin(email);
        StudentRole role = initialAdmin ? StudentRole.PRESIDENT : StudentRole.STUDENT;
        ApprovalStatus status = (initialAdmin || certificateMatched) ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING;
        if (initialAdmin) {
            log.info("최초 관리자 가입: {} → PRESIDENT", command.studentId());
        }

        // 8. PENDING이면 관리자가 확인할 수 있게 재학증명서 보관 (승인되면 보관하지 않음)
        String certificateKey = (status == ApprovalStatus.PENDING)
                ? certificateStorage.store(command.studentId(), certificate)
                : null;

        Student student = Student.builder()
                .id(command.studentId())
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(command.password()))   // BCrypt 해시로 저장
                .phoneNum(command.phoneNum())
                .role(role)
                .approvalStatus(status)
                .certificateImageKey(certificateKey)
                .approvedAt(status == ApprovalStatus.APPROVED ? LocalDateTime.now() : null)
                .build();

        // 9. 회원 저장 + 다 쓴 인증 기록 삭제를 한 트랜잭션으로
        //    저장이 실패하면(동시에 같은 학번 가입 등) 8번에서 보관한 파일도 지운다
        try {
            transactionTemplate.executeWithoutResult(tx -> {
                studentRepository.save(student);
                emailVerificationRepository.deleteAllByEmail(email);
            });
        } catch (RuntimeException e) {
            if (certificateKey != null) {
                certificateStorage.delete(certificateKey);
            }
            throw e;
        }

        return SignupResponse.from(student);
    }

    // 재학증명서에서 단과대 이름 + 이름 확인
    private boolean matchesCertificate(CertificateInfo info, String name) {
        boolean collegeMatched = info.contains(signupProperties.allowedCollege());
        boolean nameMatched = info.contains(name);
        // 이름 같은 개인정보는 로그에 남기지 않고, 맞았는지만 저장 (PENDING 원인 확인용)
        log.info("재학증명서 확인 결과: 대학 일치={}, 이름 일치={}", collegeMatched, nameMatched);
        return collegeMatched && nameMatched;
    }

    private boolean isInitialAdmin(String email) {
        String adminEmail = signupProperties.initialAdminEmail();
        return StringUtils.hasText(adminEmail)
                && email.equals(normalizeEmail(adminEmail))
                && !studentRepository.existsByRole(StudentRole.PRESIDENT);
    }

    private void validateCertificateFile(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_CERTIFICATE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_CERTIFICATE_FILE);
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_CERTIFICATE_FILE);
        }
    }

    // EmailVerificationCommandService.normalize()와 같은 규칙이어야 인증 기록을 찾을 수 있다
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

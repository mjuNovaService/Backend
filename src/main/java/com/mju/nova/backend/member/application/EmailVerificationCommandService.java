package com.mju.nova.backend.member.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.common.mail.EmailSendException;
import com.mju.nova.backend.common.mail.EmailSender;
import com.mju.nova.backend.member.application.dto.EmailSendResponse;
import com.mju.nova.backend.member.application.dto.EmailVerifyResponse;
import com.mju.nova.backend.member.domain.EmailVerification;
import com.mju.nova.backend.member.domain.EmailVerificationRepository;
import com.mju.nova.backend.member.domain.StudentRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 이메일 인증 (Command). 발송·확인 모두 email_verification에 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationCommandService {

    // 예측하기 어려운 난수 (일반 Random은 다음 값을 추측할 수 있어 인증코드에 쓰지 않는다)
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository emailVerificationRepository;
    private final StudentRepository studentRepository;
    private final EmailSender emailSender;
    private final EmailVerificationProperties properties;

    public EmailSendResponse send(String rawEmail) {
        // 1. 소문자로 정리 (Woo@MJU.ac.kr와 woo@mju.ac.kr를 같은 이메일로)
        String email = normalize(rawEmail);

        // 2. 학교 이메일만 허용
        if (!email.endsWith("@" + properties.allowedDomain())) {
            throw new BusinessException(ErrorCode.NOT_SCHOOL_EMAIL);
        }

        // 3. 이미 가입된 이메일인가 (읽기)
        if (studentRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        // 4. 같은 이메일로 방금(60초 안에) 보낸 적이 있나 (읽기)
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.findTopByEmailOrderByIdDesc(email)
                .filter(latest -> latest.getCreatedAt().isAfter(now.minus(properties.resendCooldown())))
                .ifPresent(latest -> {
                    throw new BusinessException(ErrorCode.EMAIL_SEND_TOO_FREQUENT);
                });

        // 5. 6자리 코드를 만들어 저장 (쓰기, 만료 5분)
        String code = generateCode();
        LocalDateTime expiresAt = now.plus(properties.codeExpiration());
        emailVerificationRepository.save(EmailVerification.create(email, code, expiresAt));

        // 6. 메일 발송. 실패하면 예외 → 트랜잭션이 롤백되어 5번 저장도 취소된다
        try {
            emailSender.send(email, "[NOVA] 회원가입 인증코드", buildBody(code));
        } catch (EmailSendException e) {
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }

        return new EmailSendResponse(expiresAt);
    }

    public EmailVerifyResponse verify(String rawEmail, String code) {
        // 1. 소문자로 정리 (발송 때와 같은 규칙)
        String email = normalize(rawEmail);

        // 2. 이 이메일의 가장 최근 발송 기록 (읽기). 재발송했다면 마지막 코드만 유효하다
        EmailVerification verification = emailVerificationRepository.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));

        // 3. 이미 인증했으면 그대로 성공 (확인 버튼을 두 번 눌러도 같은 결과)
        if (verification.isVerified()) {
            return EmailVerifyResponse.of(email);
        }

        // 4. 5분이 지났나
        if (verification.isExpired()) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        // 5. 코드가 맞나
        if (!verification.matches(code)) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 6. 인증 완료 기록 (쓰기, 변경 감지로 UPDATE)
        verification.markVerified();
        return EmailVerifyResponse.of(email);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // 000000 ~ 999999 (앞자리 0도 6자리로 맞춤)
    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String buildBody(String code) {
        return """
                NOVA 회원가입 인증코드입니다.

                인증코드: %s

                %d분 안에 입력해 주세요.
                본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                """.formatted(code, properties.codeExpiration().toMinutes());
    }
}

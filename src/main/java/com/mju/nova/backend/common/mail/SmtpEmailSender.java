package com.mju.nova.backend.common.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 실제 메일 발송 (local이 아닌 프로필). spring.mail.* 설정으로 Spring이 만든 JavaMailSender를 쓴다.
 */
@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;

    // 보내는 사람 = 로그인한 메일 계정
    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            log.error("메일 발송 실패: to={}", to, e);
            throw new EmailSendException("메일 발송에 실패했습니다", e);
        }
    }
}

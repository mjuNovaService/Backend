package com.mju.nova.backend.rental.infrastructure;

import com.mju.nova.backend.rental.application.MailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// TEMP(담당: A): 실제 메일을 보내지 않고 로그만 남겨 항상 성공(SENT)으로 처리된다 / A의 공통 메일 모듈이 나오면 이 클래스를 지우고 A 모듈을 MailSender 구현으로 등록한다
@Slf4j
@Component
public class LoggingMailSender implements MailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[메일 미발송: 임시 구현] to={}, subject={}", to, subject);
    }
}

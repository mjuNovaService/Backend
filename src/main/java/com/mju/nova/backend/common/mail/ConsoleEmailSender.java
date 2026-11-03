package com.mju.nova.backend.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local 전용 가짜 발송. 실제로 메일을 보내지 않고 내용을 콘솔(로그)에 찍는다.
 * 메일 계정이 없어도 인증코드 기능을 개발·테스트할 수 있다.
 */
@Slf4j
@Component
@Profile("local")
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("""

                ===== [local] 메일 발송 (실제로 보내지 않음) =====
                받는 사람: {}
                제목: {}
                {}
                ================================================""", to, subject, body);
    }
}

package com.mju.nova.backend.member.application.dto;

import java.time.LocalDateTime;

/**
 * 인증코드 발송 응답. 프론트는 expiresAt으로 남은 시간을 보여줄 수 있다.
 */
public record EmailSendResponse(LocalDateTime expiresAt) {
}

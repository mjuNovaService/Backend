package com.mju.nova.backend.member.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // 이 이메일의 가장 최근 발송 기록 (id가 클수록 나중에 만든 행)
    Optional<EmailVerification> findTopByEmailOrderByIdDesc(String email);

    // 회원가입이 끝난 이메일의 인증 기록을 지운다
    void deleteAllByEmail(String email);

    // 만료 시각이 기준보다 이전인 기록을 한 번의 DELETE로 지운다 (정리 스케줄러). 지운 개수를 돌려준다
    @Modifying
    @Query("delete from EmailVerification e where e.expiresAt < :before")
    int deleteAllExpiredBefore(@Param("before") LocalDateTime before);
}

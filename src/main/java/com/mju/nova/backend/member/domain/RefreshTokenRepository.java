package com.mju.nova.backend.member.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // 만료된 토큰을 한 번의 DELETE 쿼리로 지움 (스케줄러에서 사용). 지운 개수를 돌려준다
    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :now")
    int deleteAllExpiredBefore(@Param("now") LocalDateTime now);
}

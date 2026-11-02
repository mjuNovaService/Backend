package com.mju.nova.backend.rental.infrastructure;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 대여 기능이 쓰는 "지금 시각". LocalDateTime.now() 대신 Clock을 주입받아 쓰면 테스트에서 시각을 고정할 수 있다.
 * 반납 기한(18:00), 수령 시작 시각, 스케줄러가 모두 한국 시각 기준이라 Asia/Seoul로 고정한다.
 */
// TEMP(담당: 팀): Clock 빈을 rental 쪽에 임시로 둠 / 팀이 common에 Clock 빈을 두기로 하면 이 클래스를 지우고 그 빈을 쓴다
@Configuration
public class RentalClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}

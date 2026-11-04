package com.mju.nova.backend.locker.infrastructure;

import com.mju.nova.backend.locker.domain.Locker;
import com.mju.nova.backend.locker.domain.LockerRepository;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * local 프로필에서만 서버가 켜질 때 사물함 테스트 데이터(101~120, 20개)를 넣는다. (prod에서는 실행되지 않음)
 * 사물함이 하나라도 있으면 건너뛴다 (서버를 다시 켜도 중복으로 넣지 않음).
 * A의 테스트 계정·학기 자동 전환(둘 다 ApplicationRunner)이 끝난 뒤 발행되는 ApplicationReadyEvent에서 실행한다.
 * 신청 기간은 넣지 않는다. 테스트할 때 PATCH /api/admin/semesters/current/locker-period로 연다.
 */
// TEMP(담당: 팀): 로컬 테스트 데이터 (사물함 20개) / 팀 공통 로컬 데이터 방식이 정해지면 지우거나 그쪽으로 옮긴다
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class LockerLocalDataInitializer {

    private final LockerRepository lockerRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {
        if (lockerRepository.count() > 0) {
            return;
        }
        List<Locker> lockers = IntStream.rangeClosed(101, 120)
                .mapToObj(number -> Locker.create(String.valueOf(number)))
                .toList();
        lockerRepository.saveAll(lockers);
        log.info("[local] 테스트 사물함 {}개 생성 (101~120)", lockers.size());
    }
}

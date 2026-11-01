package com.mju.nova.backend.council.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InfoRepository extends JpaRepository<Info, Long> {

    // 메서드 이름으로 쿼리 생성: WHERE type = ? ORDER BY info_id ASC (작성 순서대로)
    List<Info> findAllByTypeOrderByIdAsc(InfoType type);
}

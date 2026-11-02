package com.mju.nova.backend.council.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Long> {

    List<Link> findAllByOrderByIdAsc();

    // 등록할 때: 같은 이름이 이미 있나?
    boolean existsByLinkName(String linkName);
}

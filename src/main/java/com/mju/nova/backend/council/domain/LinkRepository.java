package com.mju.nova.backend.council.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Long> {

    List<Link> findAllByOrderByIdAsc();

    // 등록할 때: 같은 이름이 이미 있나?
    boolean existsByLinkName(String linkName);

    // 수정할 때: 나 말고 다른 링크가 이 이름을 쓰고 있나?
    boolean existsByLinkNameAndIdNot(String linkName, Long id);
}

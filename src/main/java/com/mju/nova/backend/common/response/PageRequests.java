package com.mju.nova.backend.common.response;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 목록 API 공통 페이지 규칙. 잘못된 값이 와도 에러를 내지 않고 보정한다.
 * - page: 음수면 0
 * - size: 1보다 작으면 1, MAX_SIZE보다 크면 MAX_SIZE
 * 정렬 기준은 파트마다 달라서 호출하는 쪽이 넘긴다.
 */
public final class PageRequests {

    public static final int MAX_SIZE = 50;

    private PageRequests() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}

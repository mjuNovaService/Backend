package com.mju.nova.backend.common.response;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 목록 응답. Spring의 Page를 그대로 내보내면 필드가 너무 많고 버전마다 모양이 바뀌어서 필요한 것만 담는다.
 * page는 0부터 시작. 페이지 요청은 PageRequests로 만든다 (page·size 보정 규칙 공통).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}

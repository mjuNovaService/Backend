package com.mju.nova.backend.council.application;

import com.mju.nova.backend.council.application.dto.LinkResponse;
import com.mju.nova.backend.council.domain.LinkRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 바로가기 링크 조회 (Query).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkQueryService {

    private final LinkRepository linkRepository;

    public List<LinkResponse> getLinks() {
        return linkRepository.findAllByOrderByIdAsc().stream()
                .map(LinkResponse::from)
                .toList();
    }
}

package com.mju.nova.backend.council.application;

import com.mju.nova.backend.council.application.dto.InfoResponse;
import com.mju.nova.backend.council.domain.InfoRepository;
import com.mju.nova.backend.council.domain.InfoType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학생회 소개 조회 (Query). DB를 읽기만 함.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoQueryService {

    private final InfoRepository infoRepository;

    // 글이 없으면 빈 목록 (에러 아님)
    public List<InfoResponse> getInfos(InfoType type) {
        return infoRepository.findAllByTypeOrderByIdAsc(type).stream()
                .map(InfoResponse::from)
                .toList();
    }
}

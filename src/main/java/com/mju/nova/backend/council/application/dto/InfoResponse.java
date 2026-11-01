package com.mju.nova.backend.council.application.dto;

import com.mju.nova.backend.council.domain.Info;
import com.mju.nova.backend.council.domain.InfoType;
import java.time.LocalDateTime;

/**
 * 학생회 소개 글 1건. 수정한 관리자(updatedBy)는 학생 화면에 필요 없어서 내보내지 않는다.
 */
public record InfoResponse(
        Long infoId,
        InfoType type,
        String title,
        String content,
        LocalDateTime updatedAt
) {
    public static InfoResponse from(Info info) {
        return new InfoResponse(info.getId(), info.getType(), info.getTitle(), info.getContent(), info.getUpdatedAt());
    }
}

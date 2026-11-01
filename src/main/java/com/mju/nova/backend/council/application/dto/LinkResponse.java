package com.mju.nova.backend.council.application.dto;

import com.mju.nova.backend.council.domain.Link;

public record LinkResponse(
        Long linkId,
        String linkName,
        String url
) {
    public static LinkResponse from(Link link) {
        return new LinkResponse(link.getId(), link.getLinkName(), link.getUrl());
    }
}

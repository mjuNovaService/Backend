package com.mju.nova.backend.council.application;

import com.mju.nova.backend.common.exception.BusinessException;
import com.mju.nova.backend.common.exception.ErrorCode;
import com.mju.nova.backend.council.application.dto.LinkIdResponse;
import com.mju.nova.backend.council.domain.Link;
import com.mju.nova.backend.council.domain.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 바로가기 링크 등록·수정·삭제 (Command).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LinkCommandService {

    private final LinkRepository linkRepository;

    public LinkIdResponse create(String linkName, String url) {
        // 1차: 미리 확인해서 친절한 409
        if (linkRepository.existsByLinkName(linkName)) {
            throw new BusinessException(ErrorCode.LINK_NAME_DUPLICATED);
        }
        try {
            // saveAndFlush: INSERT를 지금 바로 보내서, 동시에 같은 이름이 들어온 경우의
            // UNIQUE 위반(uk_link_link_name)을 여기서 잡을 수 있게 한다 (2차 방어)
            Link link = linkRepository.saveAndFlush(Link.create(linkName, url));
            return new LinkIdResponse(link.getId());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.LINK_NAME_DUPLICATED);
        }
    }

    // 이름(linkName)은 못 바꾼다. 이름을 바꾸려면 삭제 후 다시 등록
    public LinkIdResponse update(Long linkId, String url) {
        Link link = findLink(linkId);
        link.changeUrl(url);
        return new LinkIdResponse(link.getId());
    }

    public void delete(Long linkId) {
        linkRepository.delete(findLink(linkId));
    }

    private Link findLink(Long linkId) {
        return linkRepository.findById(linkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LINK_NOT_FOUND));
    }
}

package com.mju.nova.backend.rental.application;

import com.mju.nova.backend.common.response.PageRequests;
import com.mju.nova.backend.common.response.PageResponse;
import com.mju.nova.backend.rental.application.dto.NotificationLogResponse;
import com.mju.nova.backend.rental.domain.NotificationLog;
import com.mju.nova.backend.rental.domain.NotificationLogRepository;
import com.mju.nova.backend.rental.domain.NotificationType;
import com.mju.nova.backend.rental.domain.SendStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 알림 발송 기록 조회 (Query).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationLogRepository notificationLogRepository;

    // 필터는 모두 선택이고 AND. date는 created_at의 날짜로 비교. 최신순
    public PageResponse<NotificationLogResponse> getNotifications(NotificationType type, SendStatus status,
                                                                  LocalDate date, int page, int size) {
        Specification<NotificationLog> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (date != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), date.atStartOfDay()));
                predicates.add(cb.lessThan(root.get("createdAt"), date.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Sort latestFirst = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        return PageResponse.of(notificationLogRepository.findAll(filter, PageRequests.of(page, size, latestFirst)),
                NotificationLogResponse::from);
    }
}

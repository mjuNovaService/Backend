package com.mju.nova.backend.rental.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RentalUnavailableEventRepository extends JpaRepository<RentalUnavailableEvent, Long> {

    // [from, to) 기간의 물품명·사유별 건수. 정렬은 서비스에서 (한글 가나다순)
    @Query("""
            select new com.mju.nova.backend.rental.domain.UnavailableEventCount(e.itemName, e.reason, count(e))
            from RentalUnavailableEvent e
            where e.createdAt >= :from and e.createdAt < :to
            group by e.itemName, e.reason
            """)
    List<UnavailableEventCount> countByItemNameAndReason(@Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);
}

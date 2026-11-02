package com.mju.nova.backend.rental.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    boolean existsByName(String name);

    boolean existsByNameAndItemNumber(String name, Integer itemNumber);

    // 같은 물품명이면 대여 일수가 같아서 아무 한 개만 보면 된다
    Optional<Item> findFirstByName(String name);

    List<Item> findByName(String name);

    // 물품명별 수량. NORMAL만 세고, availableCount는 그중 is_available = true인 것.
    // 정렬은 하지 않는다 (DB 정렬 규칙(collation)에 따라 한글이 가나다순이 아닐 수 있어 서비스에서 정렬)
    @Query("""
            select new com.mju.nova.backend.rental.domain.ItemAvailability(
                i.name, max(i.rentalPeriodDay), count(i),
                sum(case when i.isAvailable = true then 1L else 0L end))
            from Item i
            where i.status = :status
            group by i.name
            """)
    List<ItemAvailability> countByNameAndStatus(@Param("status") ItemStatus status);

    /**
     * 대여 신청 시 배정할 후보를 관리 번호 순으로 모두 잠근다 (SELECT … FOR UPDATE).
     * LIMIT 1 FOR UPDATE는 동시에 신청하면 먼저 잡힌 행이 조건에서 빠지면서 빈 결과가 나와
     * 다른 물품이 남아 있는데도 R006이 날 수 있어서, 후보 전체를 잠그고 첫 번째를 쓴다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from Item i
            where i.name = :name and i.status = :status and i.isAvailable = true
            order by i.itemNumber
            """)
    List<Item> findRentableForUpdate(@Param("name") String name, @Param("status") ItemStatus status);

    // 상태 변경·정보 수정·취소·반납이 같은 행을 동시에 고칠 때 한쪽 변경이 덮이지 않게 잠근다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);
}

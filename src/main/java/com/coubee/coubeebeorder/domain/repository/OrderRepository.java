package com.coubee.coubeebeorder.domain.repository;

import com.coubee.coubeebeorder.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * V3: 결제 완료 시점 범위로 주문 조회
     * 특정 시간 범위 내에 결제 완료된 주문들을 조회합니다.
     *
     * @param startUnix 시작 UNIX 타임스탬프
     * @param endUnix 종료 UNIX 타임스탬프
     * @param pageable 페이징 정보
     * @return 해당 시간 범위 내 결제 완료된 주문 목록
     */
    @Query("SELECT o FROM Order o WHERE o.paidAtUnix BETWEEN :startUnix AND :endUnix ORDER BY o.paidAtUnix DESC")
    Page<Order> findByPaidAtUnixBetweenOrderByPaidAtUnixDesc(@Param("startUnix") Long startUnix, @Param("endUnix") Long endUnix, Pageable pageable);

    /**
     * V3: 결제 완료된 주문 조회 (paidAtUnix가 null이 아닌 주문)
     *
     * @param pageable 페이징 정보
     * @return 결제 완료된 주문 목록
     */
    @Query("SELECT o FROM Order o WHERE o.paidAtUnix IS NOT NULL ORDER BY o.paidAtUnix DESC")
    Page<Order> findPaidOrdersOrderByPaidAtUnixDesc(Pageable pageable);

    /**
     * V3: 사용자별 결제 완료된 주문 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 해당 사용자의 결제 완료된 주문 목록
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.paidAtUnix IS NOT NULL ORDER BY o.paidAtUnix DESC")
    Page<Order> findPaidOrdersByUserIdOrderByPaidAtUnixDesc(@Param("userId") Long userId, Pageable pageable);
}
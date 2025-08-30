package com.coubee.coubeebeorder.domain.repository;

import com.coubee.coubeebeorder.domain.Order;
import com.coubee.coubeebeorder.statistic.projection.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find orders with detailed information (items and payment) by user ID
     * Uses fetch join to avoid N+1 problem when loading order details
     *
     * @param userId user ID
     * @param pageable pagination information
     * @return page of orders with items and payment details loaded
     */
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.items " +
           "LEFT JOIN FETCH o.payment " +
           "WHERE o IN :orders")
    List<Order> findWithDetailsIn(@Param("orders") List<Order> orders);

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

    // ========================================
    // Statistical Query Methods (JPA Migration from StatisticRepositoryImpl)
    // ========================================

    /**
     * Get order aggregation statistics for a date range
     * Replaces StatisticRepositoryImpl.getOrderAggregation()
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return order aggregation projection
     */
    @Query(value = """
        SELECT
            COALESCE(SUM(o.total_amount), 0) as totalSalesAmount,
            COUNT(o.order_id) as totalOrderCount,
            COUNT(DISTINCT o.user_id) as uniqueCustomerCount
        FROM orders o
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        """, nativeQuery = true)
    OrderAggregationProjection getOrderAggregation(@Param("startUnix") Long startUnix,
                                                   @Param("endUnix") Long endUnix,
                                                   @Param("storeId") Long storeId);

    /**
     * Get total item count for a date range
     * Replaces StatisticRepositoryImpl.getTotalItemCount()
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return total item count projection
     */
    @Query(value = """
        SELECT COALESCE(SUM(oi.quantity), 0) as totalItemCount
        FROM order_items oi
        WHERE EXISTS (
            SELECT 1 FROM orders o
            WHERE o.order_id = oi.order_id
            AND o.status = 'RECEIVED'
            AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
            AND (:storeId IS NULL OR o.store_id = :storeId)
        )
        """, nativeQuery = true)
    TotalItemCountProjection getTotalItemCount(@Param("startUnix") Long startUnix,
                                              @Param("endUnix") Long endUnix,
                                              @Param("storeId") Long storeId);

    /**
     * Get peak hour information for a specific date
     * Replaces StatisticRepositoryImpl.getPeakHour()
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return optional peak hour projection (empty if no data found)
     */
    @Query(value = """
        SELECT
            EXTRACT(HOUR FROM TO_TIMESTAMP(o.paid_at_unix)) as hour,
            COALESCE(SUM(o.total_amount), 0) as hourlySales
        FROM orders o
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        GROUP BY EXTRACT(HOUR FROM TO_TIMESTAMP(o.paid_at_unix))
        ORDER BY hourlySales DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<PeakHourProjection> getPeakHour(@Param("startUnix") Long startUnix,
                                            @Param("endUnix") Long endUnix,
                                            @Param("storeId") Long storeId);

    /**
     * Get best performing day for a week
     * Replaces StatisticRepositoryImpl.getBestPerformingDay()
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return optional best day projection (empty if no data found)
     */
    @Query(value = """
        SELECT
            TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY') as dayName,
            COALESCE(SUM(o.total_amount), 0) as dailySales
        FROM orders o
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        GROUP BY TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY')
        ORDER BY dailySales DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<BestDayProjection> getBestPerformingDay(@Param("startUnix") Long startUnix,
                                                    @Param("endUnix") Long endUnix,
                                                    @Param("storeId") Long storeId);

    /**
     * Get daily breakdown for a week
     * Replaces StatisticRepositoryImpl.getDailyBreakdown()
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return list of daily breakdown projections
     */
    @Query(value = """
        SELECT
            TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY') as dayOfWeek,
            DATE(TO_TIMESTAMP(o.paid_at_unix)) as orderDate,
            COALESCE(SUM(o.total_amount), 0) as salesAmount,
            COUNT(o.order_id) as orderCount
        FROM orders o
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        GROUP BY TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY'), DATE(TO_TIMESTAMP(o.paid_at_unix))
        ORDER BY orderDate
        """, nativeQuery = true)
    List<DailyBreakdownProjection> getDailyBreakdown(@Param("startUnix") Long startUnix,
                                                     @Param("endUnix") Long endUnix,
                                                     @Param("storeId") Long storeId);

    /**
     * Get top products for a month
     * Replaces StatisticRepositoryImpl.getTopProducts()
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return list of top product projections
     */
    @Query(value = """
        SELECT
            oi.product_id as productId,
            oi.product_name as productName,
            SUM(oi.quantity) as quantitySold,
            SUM(oi.quantity * oi.price) as salesAmount
        FROM order_items oi
        WHERE oi.event_type = 'PURCHASE'
        AND EXISTS (
            SELECT 1 FROM orders o
            WHERE o.order_id = oi.order_id
            AND o.status = 'RECEIVED'
            AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
            AND (:storeId IS NULL OR o.store_id = :storeId)
        )
        GROUP BY oi.product_id, oi.product_name
        ORDER BY quantitySold DESC
        LIMIT 5
        """, nativeQuery = true)
    List<TopProductProjection> getTopProducts(@Param("startUnix") Long startUnix,
                                             @Param("endUnix") Long endUnix,
                                             @Param("storeId") Long storeId);

    /**
     * Get weekly breakdown for a month
     * Replaces StatisticRepositoryImpl.getWeeklyBreakdown()
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return list of weekly breakdown projections
     */
    @Query(value = """
        SELECT
            EXTRACT(WEEK FROM TO_TIMESTAMP(o.paid_at_unix)) - EXTRACT(WEEK FROM DATE_TRUNC('month', TO_TIMESTAMP(o.paid_at_unix))) + 1 as weekNumber,
            DATE_TRUNC('week', TO_TIMESTAMP(o.paid_at_unix))::date as weekStartDate,
            (DATE_TRUNC('week', TO_TIMESTAMP(o.paid_at_unix)) + INTERVAL '6 days')::date as weekEndDate,
            COALESCE(SUM(o.total_amount), 0) as salesAmount,
            COUNT(o.order_id) as orderCount
        FROM orders o
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        GROUP BY
            EXTRACT(WEEK FROM TO_TIMESTAMP(o.paid_at_unix)) - EXTRACT(WEEK FROM DATE_TRUNC('month', TO_TIMESTAMP(o.paid_at_unix))) + 1,
            DATE_TRUNC('week', TO_TIMESTAMP(o.paid_at_unix))
        ORDER BY weekStartDate
        """, nativeQuery = true)
    List<WeeklyBreakdownProjection> getWeeklyBreakdown(@Param("startUnix") Long startUnix,
                                                       @Param("endUnix") Long endUnix,
                                                       @Param("storeId") Long storeId);

    // ========================================
    // Product Sales Summary Query Methods
    // ========================================

    /**
     * Projection interface for product sales summary
     */
    interface ProductSalesSummaryProjection {
        Long getProductId();
        String getProductName();
        Integer getQuantitySold();
        Long getTotalSalesAmount();
    }

    /**
     * Get product sales summary for a store within a date range
     * Returns products ordered by quantity sold (descending)
     *
     * @param storeId store ID to filter by
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @return list of product sales summary projections
     */
    @Query(value = "SELECT oi.product_id as productId, oi.product_name as productName, " +
                   "SUM(oi.quantity) as quantitySold, SUM(oi.quantity * oi.price) as totalSalesAmount " +
                   "FROM order_items oi JOIN orders o ON oi.order_id = o.order_id " +
                   "WHERE o.store_id = :storeId AND o.status = 'RECEIVED' AND o.paid_at_unix BETWEEN :startUnix AND :endUnix " +
                   "GROUP BY oi.product_id, oi.product_name ORDER BY quantitySold DESC", nativeQuery = true)
    List<ProductSalesSummaryProjection> findProductSalesSummaryByStore(
        @Param("storeId") Long storeId, @Param("startUnix") Long startUnix, @Param("endUnix") Long endUnix);

    // ========================================
    // User Order Summary Query Methods
    // ========================================

    /**
     * Projection interface for user order summary aggregation
     */
    interface UserOrderSummaryProjection {
        Long getTotalOrderCount();
        Long getTotalOriginalAmount();
        Long getTotalDiscountAmount();
    }

    /**
     * Get user order summary aggregation for valid orders
     * Only includes orders with status: PAID, PREPARING, PREPARED, RECEIVED
     *
     * @param userId user ID
     * @return optional user order summary projection (empty if no valid orders found)
     */
    @Query("SELECT " +
           "   count(o.id) as totalOrderCount, " +
           "   COALESCE(sum(o.originalAmount), 0L) as totalOriginalAmount, " +
           "   COALESCE(sum(o.discountAmount), 0L) as totalDiscountAmount " +
           "FROM Order o " +
           "WHERE o.userId = :userId " +
           "AND o.status IN ('PAID', 'PREPARING', 'PREPARED', 'RECEIVED')")
    Optional<UserOrderSummaryProjection> findUserOrderSummary(@Param("userId") Long userId);

    // ========================================
    // Enhanced Statistics Query Methods with Hotdeal Support
    // ========================================

    /**
     * Projection interface for comprehensive statistics with hotdeal breakdown
     */
    interface ComprehensiveStatsProjection {
        Long getTotalSalesAmount();
        Integer getTotalOrderCount();
        Integer getTotalItemCount();
        Integer getUniqueCustomerCount();
        Long getHotdealSalesAmount();
        Integer getHotdealOrderCount();
        Integer getHotdealItemCount();
        Long getRegularSalesAmount();
        Integer getRegularOrderCount();
        Integer getRegularItemCount();
    }

    /**
     * Get comprehensive statistics with hotdeal breakdown for a date range
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return comprehensive statistics projection
     */
    @Query(value = """
        SELECT
            COALESCE(SUM(o.total_amount), 0) as totalSalesAmount,
            COUNT(DISTINCT o.order_id) as totalOrderCount,
            COALESCE(SUM(oi.quantity), 0) as totalItemCount,
            COUNT(DISTINCT o.user_id) as uniqueCustomerCount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = true THEN oi.price * oi.quantity ELSE 0 END), 0) as hotdealSalesAmount,
            COUNT(DISTINCT CASE WHEN oi.was_hotdeal = true THEN o.order_id END) as hotdealOrderCount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = true THEN oi.quantity ELSE 0 END), 0) as hotdealItemCount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = false THEN oi.price * oi.quantity ELSE 0 END), 0) as regularSalesAmount,
            COUNT(DISTINCT CASE WHEN oi.was_hotdeal = false THEN o.order_id END) as regularOrderCount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = false THEN oi.quantity ELSE 0 END), 0) as regularItemCount
        FROM orders o
        JOIN order_items oi ON o.order_id = oi.order_id
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        """, nativeQuery = true)
    ComprehensiveStatsProjection getComprehensiveStatistics(@Param("startUnix") Long startUnix,
                                                           @Param("endUnix") Long endUnix,
                                                           @Param("storeId") Long storeId);

    /**
     * Projection interface for hourly breakdown with hotdeal support
     */
    interface HourlyBreakdownProjection {
        Integer getHour();
        Long getSalesAmount();
        Integer getOrderCount();
        Long getHotdealSalesAmount();
        Long getRegularSalesAmount();
    }

    /**
     * Get hourly breakdown with hotdeal analysis for a specific day
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return list of hourly breakdown projections
     */
    @Query(value = """
        SELECT
            EXTRACT(HOUR FROM TO_TIMESTAMP(o.paid_at_unix)) as hour,
            COALESCE(SUM(o.total_amount), 0) as salesAmount,
            COUNT(DISTINCT o.order_id) as orderCount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = true THEN oi.price * oi.quantity ELSE 0 END), 0) as hotdealSalesAmount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = false THEN oi.price * oi.quantity ELSE 0 END), 0) as regularSalesAmount
        FROM orders o
        JOIN order_items oi ON o.order_id = oi.order_id
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        GROUP BY EXTRACT(HOUR FROM TO_TIMESTAMP(o.paid_at_unix))
        ORDER BY hour
        """, nativeQuery = true)
    List<HourlyBreakdownProjection> getHourlyBreakdownWithHotdeal(@Param("startUnix") Long startUnix,
                                                                  @Param("endUnix") Long endUnix,
                                                                  @Param("storeId") Long storeId);

    /**
     * Projection interface for daily breakdown with hotdeal support
     */
    interface DailyBreakdownWithHotdealProjection {
        String getDayOfWeek();
        LocalDate getOrderDate();
        Long getSalesAmount();
        Integer getOrderCount();
        Long getHotdealSalesAmount();
        Long getRegularSalesAmount();
    }

    /**
     * Get daily breakdown with hotdeal analysis for a week
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return list of daily breakdown projections
     */
    @Query(value = """
        SELECT
            TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY') as dayOfWeek,
            DATE(TO_TIMESTAMP(o.paid_at_unix)) as orderDate,
            COALESCE(SUM(o.total_amount), 0) as salesAmount,
            COUNT(DISTINCT o.order_id) as orderCount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = true THEN oi.price * oi.quantity ELSE 0 END), 0) as hotdealSalesAmount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = false THEN oi.price * oi.quantity ELSE 0 END), 0) as regularSalesAmount
        FROM orders o
        JOIN order_items oi ON o.order_id = oi.order_id
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        GROUP BY TO_CHAR(TO_TIMESTAMP(o.paid_at_unix), 'DAY'), DATE(TO_TIMESTAMP(o.paid_at_unix))
        ORDER BY orderDate
        """, nativeQuery = true)
    List<DailyBreakdownWithHotdealProjection> getDailyBreakdownWithHotdeal(@Param("startUnix") Long startUnix,
                                                                           @Param("endUnix") Long endUnix,
                                                                           @Param("storeId") Long storeId);

    /**
     * Projection interface for weekly breakdown with hotdeal support
     */
    interface WeeklyBreakdownWithHotdealProjection {
        Integer getWeekNumber();
        LocalDate getWeekStartDate();
        LocalDate getWeekEndDate();
        Long getSalesAmount();
        Integer getOrderCount();
        Long getHotdealSalesAmount();
        Long getRegularSalesAmount();
    }

    /**
     * Get weekly breakdown with hotdeal analysis for a month
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return list of weekly breakdown projections
     */
    @Query(value = """
        SELECT
            EXTRACT(WEEK FROM TO_TIMESTAMP(o.paid_at_unix)) - EXTRACT(WEEK FROM DATE_TRUNC('month', TO_TIMESTAMP(o.paid_at_unix))) + 1 as weekNumber,
            DATE_TRUNC('week', TO_TIMESTAMP(o.paid_at_unix))::date as weekStartDate,
            (DATE_TRUNC('week', TO_TIMESTAMP(o.paid_at_unix)) + INTERVAL '6 days')::date as weekEndDate,
            COALESCE(SUM(o.total_amount), 0) as salesAmount,
            COUNT(DISTINCT o.order_id) as orderCount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = true THEN oi.price * oi.quantity ELSE 0 END), 0) as hotdealSalesAmount,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = false THEN oi.price * oi.quantity ELSE 0 END), 0) as regularSalesAmount
        FROM orders o
        JOIN order_items oi ON o.order_id = oi.order_id
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        GROUP BY EXTRACT(WEEK FROM TO_TIMESTAMP(o.paid_at_unix)), DATE_TRUNC('week', TO_TIMESTAMP(o.paid_at_unix))
        ORDER BY weekStartDate
        """, nativeQuery = true)
    List<WeeklyBreakdownWithHotdealProjection> getWeeklyBreakdownWithHotdeal(@Param("startUnix") Long startUnix,
                                                                             @Param("endUnix") Long endUnix,
                                                                             @Param("storeId") Long storeId);

    /**
     * Projection interface for sold items summary with hotdeal breakdown
     */
    interface SoldItemsSummaryProjection {
        Long getProductId();
        String getProductName();
        Integer getTotalQuantity();
        Long getTotalRevenue();
        Integer getHotdealQuantity();
        Long getHotdealRevenue();
        Integer getRegularQuantity();
        Long getRegularRevenue();
        Boolean getWasPartOfHotdeal();
    }

    /**
     * Get sold items summary with hotdeal breakdown for a date range
     *
     * @param startUnix start timestamp (UNIX)
     * @param endUnix end timestamp (UNIX)
     * @param storeId store ID filter (optional)
     * @return list of sold items summary projections
     */
    @Query(value = """
        SELECT
            oi.product_id as productId,
            oi.product_name as productName,
            SUM(oi.quantity) as totalQuantity,
            SUM(oi.price * oi.quantity) as totalRevenue,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = true THEN oi.quantity ELSE 0 END), 0) as hotdealQuantity,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = true THEN oi.price * oi.quantity ELSE 0 END), 0) as hotdealRevenue,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = false THEN oi.quantity ELSE 0 END), 0) as regularQuantity,
            COALESCE(SUM(CASE WHEN oi.was_hotdeal = false THEN oi.price * oi.quantity ELSE 0 END), 0) as regularRevenue,
            CASE WHEN SUM(CASE WHEN oi.was_hotdeal = true THEN 1 ELSE 0 END) > 0 THEN true ELSE false END as wasPartOfHotdeal
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.order_id
        WHERE o.status = 'RECEIVED'
        AND o.paid_at_unix BETWEEN :startUnix AND :endUnix
        AND (:storeId IS NULL OR o.store_id = :storeId)
        GROUP BY oi.product_id, oi.product_name
        ORDER BY totalRevenue DESC
        """, nativeQuery = true)
    List<SoldItemsSummaryProjection> getSoldItemsSummaryWithHotdeal(@Param("startUnix") Long startUnix,
                                                                    @Param("endUnix") Long endUnix,
                                                                    @Param("storeId") Long storeId);
}
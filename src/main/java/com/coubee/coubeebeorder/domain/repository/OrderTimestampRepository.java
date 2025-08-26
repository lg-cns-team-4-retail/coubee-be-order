package com.coubee.coubeebeorder.domain.repository;

import com.coubee.coubeebeorder.domain.OrderTimestamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTimestampRepository extends JpaRepository<OrderTimestamp, Long> {

    /**
     * Find all status history for a given order, sorted by timestamp ascending
     *
     * @param orderId the order ID (string format)
     * @return list of order timestamps sorted by updatedAt ascending
     */
    @Query("SELECT ot FROM OrderTimestamp ot WHERE ot.order.orderId = :orderId ORDER BY ot.updatedAt ASC")
    List<OrderTimestamp> findByOrderIdOrderByUpdatedAtAsc(@Param("orderId") String orderId);

    /**
     * Find all status history for a given order entity, sorted by timestamp ascending
     *
     * @param orderId the order entity ID (Long format)
     * @return list of order timestamps sorted by updatedAt ascending
     */
    @Query("SELECT ot FROM OrderTimestamp ot WHERE ot.order.id = :orderId ORDER BY ot.updatedAt ASC")
    List<OrderTimestamp> findByOrderEntityIdOrderByUpdatedAtAsc(@Param("orderId") Long orderId);
}

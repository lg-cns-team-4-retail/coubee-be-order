package com.coubee.coubeebeorder.domain.repository;

import com.coubee.coubeebeorder.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderId(String orderId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
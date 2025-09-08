package com.coubee.coubeebeorder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pending Order Cleanup Scheduler
 * 
 * This component automatically cleans up stale pending orders to prevent "ghost stock" issues.
 * Orders that remain in PENDING status for more than 15 minutes are considered abandoned
 * and are automatically cancelled to release reserved inventory.
 * 
 * Key Features:
 * - Runs every 10 minutes (600,000 milliseconds)
 * - Identifies orders in PENDING status older than 15 minutes
 * - Changes order status to FAILED
 * - Publishes stock restoration events via Kafka
 * - Maintains audit trail through order history
 * 
 * This mechanism ensures optimal inventory utilization and prevents lost sales
 * due to unnecessarily reserved stock from abandoned orders.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    // It's better to call the OrderService method to clarify transaction boundaries.
    private final OrderService orderService; 

    /**
     * Scheduled method that runs every 10 minutes to clean up stale pending orders.
     * 
     * This method delegates the actual cleanup logic to OrderService.cancelStalePendingOrders()
     * to maintain proper transaction boundaries and separation of concerns.
     * 
     * Execution frequency: Every 10 minutes (fixedRate = 600000 milliseconds)
     * - fixedRate ensures consistent execution intervals regardless of method execution time
     * - If cleanup takes longer than 10 minutes, the next execution will start immediately after completion
     */
    @Scheduled(fixedRate = 600000) // Runs every 10 minutes (fixedRate = 600000 milliseconds)
    public void cleanupPendingOrders() {
        log.info("Starting cleanup job for stale pending orders.");
        
        try {
            orderService.cancelStalePendingOrders();
            log.info("Completed cleanup job for stale pending orders.");
        } catch (Exception e) {
            log.error("Error occurred during pending orders cleanup job", e);
            // Don't rethrow - we want the scheduler to continue running
        }
    }
}

package com.coubee.coubeebeorder.domain.repository;

import com.coubee.coubeebeorder.domain.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, String> {
    
    // The primary key is webhookId, so we inherit findById(String webhookId) from JpaRepository
    // Additional methods can be added here if needed for cleanup or monitoring
}

package com.coubee.coubeebeorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "processed_webhooks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProcessedWebhook {

    @Id
    @Column(name = "webhook_id", nullable = false)
    private String webhookId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Builder
    private ProcessedWebhook(String webhookId, LocalDateTime processedAt) {
        this.webhookId = webhookId;
        this.processedAt = processedAt;
    }

    public static ProcessedWebhook create(String webhookId) {
        return ProcessedWebhook.builder()
                .webhookId(webhookId)
                .processedAt(LocalDateTime.now())
                .build();
    }
}

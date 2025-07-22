package com.coubee.kafka.producer.notification.event;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Notification Event
 */
@Getter
@Builder
public class NotificationEvent {

    /**
     * User ID
     */
    private Long userId;

    /**
     * Notification title
     */
    private String title;

    /**
     * Notification body
     */
    private String body;

    /**
     * Notification type (ORDER, PAYMENT etc)
     */
    private String type;

    /**
     * Additional notification data
     */
    private Map<String, String> data;
} 

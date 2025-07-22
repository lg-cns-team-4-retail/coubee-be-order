package com.coubee.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * PortOne V2 Configuration Properties
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portone.v2")
public class PortOneProperties {

    /**
     * PortOne Store ID
     */
    private String storeId;

    /**
     * PortOne API Secret
     */
    private String apiSecret;

    /**
     * Payment channel keys by payment method
     * Key: payment method (e.g., "kakaopay", "tosspayments")
     * Value: channel key for that payment method
     */
    private Map<String, String> channels;

    /**
     * Get channel key for specific payment method
     */
    public String getChannelKey(String payMethod) {
        if (channels == null || !channels.containsKey(payMethod.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported payment method or channel key not configured: " + payMethod);
        }
        return channels.get(payMethod.toLowerCase());
    }
}
package com.coubee.coubeebeorder.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portone.v2")
public class PortOneProperties {

    private String storeId;

    private String apiSecret;

    private Map<String, String> channels;

    public String getChannelKey(String payMethod) {
        if (channels == null || !channels.containsKey(payMethod.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported payment method or channel key not configured: " + payMethod);
        }
        return channels.get(payMethod.toLowerCase());
    }
}
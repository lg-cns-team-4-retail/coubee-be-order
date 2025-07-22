package com.coubee.kafka.producer;

import com.coubee.kafka.message.ItemPurchasedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!dev") // dev 프로필이 아닐 때만 이 Bean을 생성
public class ItemPurchasedProducer {

    private static final String TOPIC = "item-purchased-topic";
    private final KafkaTemplate<String, ItemPurchasedMessage> kafkaTemplate;

    public void sendMessage(ItemPurchasedMessage message) {
        kafkaTemplate.send(TOPIC, message);
    }
} 

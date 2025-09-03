package com.coubee.coubeebeorder.service;

public record OrderPaidEvent(String orderId, Long storeId, Long userId) {
}

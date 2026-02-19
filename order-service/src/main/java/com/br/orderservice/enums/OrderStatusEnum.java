package com.br.orderservice.enums;

public enum OrderStatusEnum {
    PENDING_PAYMENT,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    REFUNDED,
    CANCELLED
}

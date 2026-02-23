package com.br.orderservice.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedItemEvent(
    UUID productId,
    String productName,
    String productSku,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice
) {}

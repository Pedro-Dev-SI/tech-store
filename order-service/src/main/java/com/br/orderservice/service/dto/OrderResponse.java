package com.br.orderservice.service.dto;

import com.br.orderservice.enums.OrderStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String orderNumber,
    UUID userId,
    OrderStatusEnum status,
    BigDecimal totalAmount,
    String shippingAddress,
    String notes,
    List<OrderItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

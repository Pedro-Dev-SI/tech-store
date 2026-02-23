package com.br.orderservice.service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String slug,
        String description,
        String brand,
        UUID categoryId,
        BigDecimal price,
        BigDecimal compareAtPrice,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

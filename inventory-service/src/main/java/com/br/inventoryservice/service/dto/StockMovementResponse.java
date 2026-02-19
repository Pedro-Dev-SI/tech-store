package com.br.inventoryservice.service.dto;

import com.br.inventoryservice.enums.StockMovementType;

import java.util.UUID;

public record StockMovementResponse(
    UUID id,
    UUID inventoryId,
    StockMovementType type,
    Integer quantity,
    String reason,
    UUID orderId
) {}

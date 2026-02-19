package com.br.inventoryservice.service.dto;

import java.util.UUID;

public record InventoryResponse(
    UUID id,
    UUID productId,
    Integer quantity,
    Integer reservedQuantity,
    Integer minStockAlert
) {}

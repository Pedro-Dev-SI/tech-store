package com.br.inventoryservice.event;

import java.time.Instant;
import java.util.UUID;

public record StockLowAlertEvent(
    UUID eventId,
    Instant occurredAt,
    UUID productId,
    int availableQuantity,
    int minimumQuantity
) {}

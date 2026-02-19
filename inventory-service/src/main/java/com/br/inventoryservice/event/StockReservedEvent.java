package com.br.inventoryservice.event;

import java.time.Instant;
import java.util.UUID;

public record StockReservedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID orderId,
    UUID productId,
    int quantity
) {}

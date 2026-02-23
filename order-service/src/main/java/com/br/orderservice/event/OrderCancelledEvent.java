package com.br.orderservice.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
    UUID eventId,
    Instant occurredAt,
    UUID orderId,
    String reason
) {}

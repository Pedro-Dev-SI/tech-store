package com.br.orderservice.event;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
    UUID eventId,
    Instant occurredAt,
    UUID orderId,
    String trackingCode
) {}

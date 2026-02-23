package com.br.orderservice.event;

import java.time.Instant;
import java.util.UUID;

public record OrderPaidEvent(
    UUID eventId,
    Instant occurredAt,
    UUID orderId
) {}

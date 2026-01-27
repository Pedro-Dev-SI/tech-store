package com.br.productservice.service.dto;

import java.util.UUID;

public record ProductAttributesResponse(
    UUID id,
    UUID productId,
    String name,
    String value
) {
}

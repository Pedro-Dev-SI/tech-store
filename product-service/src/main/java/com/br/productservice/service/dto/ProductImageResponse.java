package com.br.productservice.service.dto;

import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String url,
        String altText,
        Integer position,
        UUID productId,
        Boolean isMain
) {
}

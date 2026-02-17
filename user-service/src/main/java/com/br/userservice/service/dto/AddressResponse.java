package com.br.userservice.service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AddressResponse(
        UUID userId,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode,
        Boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

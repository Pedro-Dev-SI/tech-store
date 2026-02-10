package com.br.userservice.service.dto;

import java.util.UUID;

public record LoginResponse(
    UUID id,
    String email,
    String role,
    String status
) {
}

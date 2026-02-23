package com.br.orderservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class InventoryReserveItemRequest {
    private UUID productId;
    private Integer quantity;
}

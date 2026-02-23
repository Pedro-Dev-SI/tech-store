package com.br.orderservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class InventoryReserveRequest {
    private UUID orderId;
    private List<InventoryReserveItemRequest> items;
}

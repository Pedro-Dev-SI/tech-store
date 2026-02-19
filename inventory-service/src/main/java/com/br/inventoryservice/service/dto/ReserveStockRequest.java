package com.br.inventoryservice.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class ReserveStockRequest {

    @NotNull
    private UUID orderId;

    @Valid
    @NotEmpty
    private List<StockItemRequest> items;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public List<StockItemRequest> getItems() {
        return items;
    }

    public void setItems(List<StockItemRequest> items) {
        this.items = items;
    }
}

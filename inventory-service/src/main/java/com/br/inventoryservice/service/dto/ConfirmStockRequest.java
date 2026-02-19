package com.br.inventoryservice.service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ConfirmStockRequest {

    @NotNull
    private UUID orderId;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
}

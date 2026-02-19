package com.br.orderservice.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateOrderRequest {

    @NotEmpty
    private List<@Valid OrderItemRequest> items;

    @NotNull
    private UUID addressId;

    private String notes;
}

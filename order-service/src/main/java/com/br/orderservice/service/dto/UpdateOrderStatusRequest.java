package com.br.orderservice.service.dto;

import com.br.orderservice.enums.OrderStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequest {

    @NotNull
    private OrderStatusEnum status;

    private String notes;
}

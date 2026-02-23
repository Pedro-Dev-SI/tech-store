package com.br.orderservice.service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderRequest {
    private String reason;
}

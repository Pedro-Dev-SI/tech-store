package com.br.orderservice.service.mapper;

import com.br.orderservice.model.Order;
import com.br.orderservice.model.OrderItem;
import com.br.orderservice.service.dto.OrderItemResponse;
import com.br.orderservice.service.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "items", target = "items")
    OrderResponse toResponse(Order order, List<OrderItem> items);

    OrderItemResponse toItemResponse(OrderItem item);
}

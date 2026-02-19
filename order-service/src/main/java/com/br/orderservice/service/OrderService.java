package com.br.orderservice.service;

import com.br.orderservice.client.UserServiceClient;
import com.br.orderservice.exception.BusinessException;
import com.br.orderservice.repository.OrderRepository;
import com.br.orderservice.service.dto.AddressResponse;
import com.br.orderservice.service.dto.CreateOrderRequest;
import com.br.orderservice.service.dto.OrderResponse;
import com.br.orderservice.service.dto.UserResponse;
import com.br.orderservice.service.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;

    public OrderService(OrderRepository orderRepository, OrderMapper orderMapper, UserServiceClient userServiceClient) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.userServiceClient = userServiceClient;
    }

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest createOrderRequest) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        UserResponse userResponse = null;

        try{
            userResponse = userServiceClient.getCurrentUser(userId);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Problem finding user to create an order", e);
        }

        if (createOrderRequest.getItems().isEmpty()) {
            throw new BusinessException("To create an order is required to have at least one item linked");
        }

        var items = createOrderRequest.getItems();

        for (var item : items) {
            if (item.getQuantity() <= 0) {
                throw new BusinessException("Quantity for the each item must be greater than 0");
            }
        }

        AddressResponse validDefaultAddress = null;

        try {
            validDefaultAddress = userServiceClient.getAddressById(createOrderRequest.getAddressId());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Problem finding a valid address for the user", e);
        }


    }
}

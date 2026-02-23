package com.br.orderservice.service;

import com.br.orderservice.client.InventoryServiceClient;
import com.br.orderservice.client.ProductServiceClient;
import com.br.orderservice.client.UserServiceClient;
import com.br.orderservice.enums.OrderStatusEnum;
import com.br.orderservice.event.OrderEventProducer;
import com.br.orderservice.exception.BusinessException;
import com.br.orderservice.exception.ForbiddenException;
import com.br.orderservice.exception.ResourceNotFoundException;
import com.br.orderservice.model.Order;
import com.br.orderservice.model.OrderItem;
import com.br.orderservice.repository.OrderItemRepository;
import com.br.orderservice.repository.OrderRepository;
import com.br.orderservice.repository.OrderStatusHistoryRepository;
import com.br.orderservice.service.dto.AddressResponse;
import com.br.orderservice.service.dto.CreateOrderRequest;
import com.br.orderservice.service.dto.OrderItemRequest;
import com.br.orderservice.service.dto.OrderResponse;
import com.br.orderservice.service.dto.ProductResponse;
import com.br.orderservice.service.dto.UpdateOrderStatusRequest;
import com.br.orderservice.service.dto.UserResponse;
import com.br.orderservice.service.mapper.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private ProductServiceClient productServiceClient;
    @Mock
    private InventoryServiceClient inventoryServiceClient;
    @Mock
    private OrderEventProducer orderEventProducer;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
            orderRepository,
            orderItemRepository,
            orderStatusHistoryRepository,
            orderMapper,
            userServiceClient,
            productServiceClient,
            inventoryServiceClient,
            orderEventProducer,
            new ObjectMapper()
        );
    }

    @Test
    void createOrder_success() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CreateOrderRequest request = buildCreateOrderRequest(addressId, productId, 2);
        UserResponse user = new UserResponse(userId, "email@test.com", "123", "999", "Name", "USER", "ACTIVE");
        AddressResponse address = new AddressResponse(
            userId, "Street", "10", null, "Center", "Sao Paulo", "SP", "00000-000", true,
            LocalDateTime.now(), LocalDateTime.now()
        );
        ProductResponse product = new ProductResponse(
            productId, "SKU-1", "Product", "product", "desc", "brand",
            UUID.randomUUID(), BigDecimal.TEN, BigDecimal.ZERO, true, LocalDateTime.now(), LocalDateTime.now()
        );

        when(userServiceClient.getCurrentUser(userId)).thenReturn(user);
        when(userServiceClient.getAddressById(userId, addressId)).thenReturn(address);
        when(productServiceClient.getAllByListIds(any())).thenReturn(List.of(product));

        Order savedOrder = new Order();
        savedOrder.setId(orderId);
        savedOrder.setUserId(userId);
        savedOrder.setStatus(OrderStatusEnum.PENDING_PAYMENT);
        savedOrder.setTotalAmount(BigDecimal.valueOf(20));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse expected = new OrderResponse(
            orderId, "TS-TEST", userId, OrderStatusEnum.PENDING_PAYMENT, BigDecimal.valueOf(20),
            "{\"street\":\"Street\"}", null, List.of(), LocalDateTime.now(), LocalDateTime.now()
        );
        when(orderMapper.toResponse(any(Order.class), any())).thenReturn(expected);

        OrderResponse response = orderService.createOrder(userId, request);

        assertNotNull(response);
        assertEquals(orderId, response.id());
        verify(inventoryServiceClient, times(1)).reserveStock(eq("true"), any());
        verify(orderEventProducer, times(1)).publishOrderCreated(any());
        verify(orderStatusHistoryRepository, times(1)).save(any());
    }

    @Test
    void createOrder_nullUserId_throws() {
        CreateOrderRequest request = buildCreateOrderRequest(UUID.randomUUID(), UUID.randomUUID(), 1);
        assertThrows(BusinessException.class, () -> orderService.createOrder(null, request));
    }

    @Test
    void createOrder_emptyItems_throws() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddressId(UUID.randomUUID());
        request.setItems(List.of());
        assertThrows(BusinessException.class, () -> orderService.createOrder(UUID.randomUUID(), request));
    }

    @Test
    void createOrder_invalidQuantity_throws() {
        CreateOrderRequest request = buildCreateOrderRequest(UUID.randomUUID(), UUID.randomUUID(), 0);
        assertThrows(BusinessException.class, () -> orderService.createOrder(UUID.randomUUID(), request));
    }

    @Test
    void createOrder_missingInactiveProducts_throws() {
        UUID userId = UUID.randomUUID();
        UUID addressId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(userServiceClient.getCurrentUser(userId)).thenReturn(
            new UserResponse(userId, "email@test.com", "123", "999", "Name", "USER", "ACTIVE")
        );
        when(userServiceClient.getAddressById(userId, addressId)).thenReturn(
            new AddressResponse(userId, "Street", "10", null, "Center", "Sao Paulo", "SP", "00000-000", true,
                LocalDateTime.now(), LocalDateTime.now())
        );
        when(productServiceClient.getAllByListIds(any())).thenReturn(List.of());

        CreateOrderRequest request = buildCreateOrderRequest(addressId, productId, 1);

        assertThrows(BusinessException.class, () -> orderService.createOrder(userId, request));
    }

    @Test
    void getOrderById_ownerSuccess() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = buildOrder(orderId, userId, OrderStatusEnum.PENDING_PAYMENT);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(any(), any())).thenReturn(
            new OrderResponse(orderId, "TS", userId, OrderStatusEnum.PENDING_PAYMENT, BigDecimal.ONE, "{}", null, List.of(), null, null)
        );

        OrderResponse response = orderService.getOrderById(orderId, userId, "USER");
        assertEquals(orderId, response.id());
    }

    @Test
    void getOrderById_nonOwnerNonAdmin_throws() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, UUID.randomUUID(), OrderStatusEnum.PENDING_PAYMENT);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(ForbiddenException.class, () -> orderService.getOrderById(orderId, UUID.randomUUID(), "USER"));
    }

    @Test
    void getOrderById_notFound_throws() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(orderId, UUID.randomUUID(), "ADMIN"));
    }

    @Test
    void cancelOrder_userPendingPayment_successAndReleaseStock() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = buildOrder(orderId, userId, OrderStatusEnum.PENDING_PAYMENT);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(any(), any())).thenReturn(
            new OrderResponse(orderId, "TS", userId, OrderStatusEnum.CANCELLED, BigDecimal.ONE, "{}", null, List.of(), null, null)
        );

        OrderResponse response = orderService.cancelOrder(orderId, userId, "USER", "Client requested");
        assertEquals(OrderStatusEnum.CANCELLED, response.status());
        verify(inventoryServiceClient, times(1)).releaseStock(eq("true"), any());
        verify(orderEventProducer, times(1)).publishOrderCancelled(any());
    }

    @Test
    void cancelOrder_userCannotCancelNonPending_throws() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = buildOrder(orderId, userId, OrderStatusEnum.PROCESSING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> orderService.cancelOrder(orderId, userId, "USER", null));
    }

    @Test
    void cancelOrder_adminProcessing_successWithoutReleaseCall() {
        UUID orderId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Order order = buildOrder(orderId, ownerId, OrderStatusEnum.PROCESSING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(any(), any())).thenReturn(
            new OrderResponse(orderId, "TS", ownerId, OrderStatusEnum.CANCELLED, BigDecimal.ONE, "{}", null, List.of(), null, null)
        );

        orderService.cancelOrder(orderId, adminId, "ADMIN", null);

        verify(inventoryServiceClient, times(0)).releaseStock(eq("true"), any());
        verify(orderEventProducer, times(1)).publishOrderCancelled(any());
    }

    @Test
    void updateOrderStatus_invalidTransition_throws() {
        UUID orderId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Order order = buildOrder(orderId, UUID.randomUUID(), OrderStatusEnum.PENDING_PAYMENT);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatusEnum.SHIPPED);

        assertThrows(BusinessException.class, () -> orderService.updateOrderStatus(orderId, actor, request));
    }

    @Test
    void updateOrderStatus_paymentConfirmed_callsInventoryConfirmAndPublishesPaidEvent() {
        UUID orderId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Order order = buildOrder(orderId, UUID.randomUUID(), OrderStatusEnum.PENDING_PAYMENT);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(any(), any())).thenReturn(
            new OrderResponse(orderId, "TS", order.getUserId(), OrderStatusEnum.PAYMENT_CONFIRMED, BigDecimal.ONE, "{}", null, List.of(), null, null)
        );

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatusEnum.PAYMENT_CONFIRMED);
        request.setNotes("Payment approved");

        orderService.updateOrderStatus(orderId, actor, request);

        verify(inventoryServiceClient, times(1)).confirmStock(eq("true"), any());
        verify(orderEventProducer, times(1)).publishOrderPaid(any());
    }

    @Test
    void updateOrderStatus_shipped_publishesShippedEvent() {
        UUID orderId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        Order order = buildOrder(orderId, UUID.randomUUID(), OrderStatusEnum.PROCESSING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(any(), any())).thenReturn(
            new OrderResponse(orderId, "TS", order.getUserId(), OrderStatusEnum.SHIPPED, BigDecimal.ONE, "{}", null, List.of(), null, null)
        );

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatusEnum.SHIPPED);

        orderService.updateOrderStatus(orderId, actor, request);

        verify(orderEventProducer, times(1)).publishOrderShipped(any());
    }

    @Test
    void listMyOrders_returnsMappedPage() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Order order = buildOrder(orderId, userId, OrderStatusEnum.PENDING_PAYMENT);
        when(orderRepository.findByUserId(userId, pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 1));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(any(), any())).thenReturn(
            new OrderResponse(orderId, "TS", userId, OrderStatusEnum.PENDING_PAYMENT, BigDecimal.ONE, "{}", null, List.of(), null, null)
        );

        Page<OrderResponse> page = orderService.listMyOrders(userId, pageable);
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void listAllOrders_returnsMappedPage() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Order order = buildOrder(orderId, userId, OrderStatusEnum.PENDING_PAYMENT);

        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order), pageable, 1));
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderMapper.toResponse(any(), any())).thenReturn(
            new OrderResponse(orderId, "TS", userId, OrderStatusEnum.PENDING_PAYMENT, BigDecimal.ONE, "{}", null, List.of(), null, null)
        );

        Page<OrderResponse> page = orderService.listAllOrders(pageable);
        assertEquals(1, page.getTotalElements());
    }

    private CreateOrderRequest buildCreateOrderRequest(UUID addressId, UUID productId, Integer quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setAddressId(addressId);
        request.setItems(List.of(item));
        return request;
    }

    private Order buildOrder(UUID orderId, UUID userId, OrderStatusEnum status) {
        Order order = new Order();
        order.setId(orderId);
        order.setUserId(userId);
        order.setOrderNumber("TS-20260223-ABCDE");
        order.setStatus(status);
        order.setTotalAmount(BigDecimal.ONE);
        order.setShippingAddress("{}");
        return order;
    }
}

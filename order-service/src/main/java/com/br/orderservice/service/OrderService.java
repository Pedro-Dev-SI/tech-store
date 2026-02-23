package com.br.orderservice.service;

import com.br.orderservice.client.InventoryServiceClient;
import com.br.orderservice.client.ProductServiceClient;
import com.br.orderservice.client.UserServiceClient;
import com.br.orderservice.enums.OrderStatusEnum;
import com.br.orderservice.event.OrderCancelledEvent;
import com.br.orderservice.event.OrderCreatedEvent;
import com.br.orderservice.event.OrderCreatedItemEvent;
import com.br.orderservice.event.OrderEventProducer;
import com.br.orderservice.event.OrderPaidEvent;
import com.br.orderservice.event.OrderShippedEvent;
import com.br.orderservice.exception.BusinessException;
import com.br.orderservice.exception.ForbiddenException;
import com.br.orderservice.exception.ResourceNotFoundException;
import com.br.orderservice.model.Order;
import com.br.orderservice.model.OrderItem;
import com.br.orderservice.model.OrderStatusHistory;
import com.br.orderservice.repository.OrderItemRepository;
import com.br.orderservice.repository.OrderRepository;
import com.br.orderservice.repository.OrderStatusHistoryRepository;
import com.br.orderservice.service.dto.AddressResponse;
import com.br.orderservice.service.dto.CreateOrderRequest;
import com.br.orderservice.service.dto.InventoryOrderRequest;
import com.br.orderservice.service.dto.InventoryReserveItemRequest;
import com.br.orderservice.service.dto.InventoryReserveRequest;
import com.br.orderservice.service.dto.OrderItemRequest;
import com.br.orderservice.service.dto.OrderResponse;
import com.br.orderservice.service.dto.ProductResponse;
import com.br.orderservice.service.dto.UpdateOrderStatusRequest;
import com.br.orderservice.service.dto.UserResponse;
import com.br.orderservice.service.mapper.OrderMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final String INTERNAL_CALL_HEADER_VALUE = "true";
    private static final String ORDER_NUMBER_PREFIX = "TS";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderMapper orderMapper;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        OrderStatusHistoryRepository orderStatusHistoryRepository,
        OrderMapper orderMapper,
        UserServiceClient userServiceClient,
        ProductServiceClient productServiceClient,
        InventoryServiceClient inventoryServiceClient,
        OrderEventProducer orderEventProducer,
        ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.orderMapper = orderMapper;
        this.userServiceClient = userServiceClient;
        this.productServiceClient = productServiceClient;
        this.inventoryServiceClient = inventoryServiceClient;
        this.orderEventProducer = orderEventProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates an order using snapshots for address and product data.
     */
    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        if (userId == null) {
            throw new BusinessException("User id must not be null");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("To create an order, at least one item is required");
        }
        request.getItems().forEach(item -> {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException("Quantity for each item must be greater than 0");
            }
        });

        UserResponse user = fetchUserOrThrow(userId);
        AddressResponse address = fetchAddressOrThrow(userId, request.getAddressId());
        String shippingAddressSnapshot = toJson(address);

        List<ProductResponse> activeProducts = fetchAndValidateActiveProducts(request.getItems());
        Map<UUID, ProductResponse> productById = activeProducts.stream()
            .collect(Collectors.toMap(ProductResponse::id, product -> product));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest requestedItem : request.getItems()) {
            ProductResponse product = productById.get(requestedItem.getProductId());
            BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(requestedItem.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.id());
            orderItem.setProductName(product.name());
            orderItem.setProductSku(product.sku());
            orderItem.setQuantity(requestedItem.getQuantity());
            orderItem.setUnitPrice(product.price());
            orderItem.setTotalPrice(itemTotal);
            orderItems.add(orderItem);

            totalAmount = totalAmount.add(itemTotal);
        }

        Order order = new Order();
        order.setUserId(user.id());
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatusEnum.PENDING_PAYMENT);
        order.setTotalAmount(totalAmount);
        order.setShippingAddress(shippingAddressSnapshot);
        order.setNotes(request.getNotes());

        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            item.setOrderId(savedOrder.getId());
        }
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

        saveStatusHistory(savedOrder.getId(), null, OrderStatusEnum.PENDING_PAYMENT, "Order created", user.id());

        reserveStock(savedOrder.getId(), request.getItems());
        publishOrderCreatedEvent(savedOrder, savedItems);

        return orderMapper.toResponse(savedOrder, savedItems);
    }

    /**
     * Returns paginated orders for the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> listMyOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
            .map(this::toResponseWithItems);
    }

    /**
     * Returns paginated orders for admins.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> listAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponseWithItems);
    }

    /**
     * Returns one order if caller is owner or admin.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, UUID userId, String role) {
        Order order = findOrderOrThrow(orderId);
        validateOwnerOrAdmin(order, userId, role);
        return toResponseWithItems(order);
    }

    /**
     * Cancels an order with role-aware business rules.
     */
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId, String role, String reason) {
        Order order = findOrderOrThrow(orderId);
        boolean admin = isAdmin(role);

        if (!admin && !order.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        if (admin) {
            if (!(order.getStatus() == OrderStatusEnum.PENDING_PAYMENT
                || order.getStatus() == OrderStatusEnum.PAYMENT_CONFIRMED
                || order.getStatus() == OrderStatusEnum.PROCESSING)) {
                throw new BusinessException("Admin cannot cancel order with status: " + order.getStatus());
            }
        } else if (order.getStatus() != OrderStatusEnum.PENDING_PAYMENT) {
            throw new BusinessException("User can cancel only orders in PENDING_PAYMENT");
        }

        OrderStatusEnum previousStatus = order.getStatus();
        order.setStatus(OrderStatusEnum.CANCELLED);
        Order saved = orderRepository.save(order);

        // Release reservation only when stock is expected to still be reserved.
        if (previousStatus == OrderStatusEnum.PENDING_PAYMENT || previousStatus == OrderStatusEnum.PAYMENT_CONFIRMED) {
            inventoryServiceClient.releaseStock(INTERNAL_CALL_HEADER_VALUE, new InventoryOrderRequest(orderId));
        }

        saveStatusHistory(
            orderId,
            previousStatus,
            OrderStatusEnum.CANCELLED,
            normalizeNotes(reason, "Order cancelled"),
            userId
        );

        orderEventProducer.publishOrderCancelled(new OrderCancelledEvent(
            UUID.randomUUID(),
            Instant.now(),
            orderId,
            normalizeNotes(reason, "Order cancelled")
        ));

        return toResponseWithItems(saved);
    }

    /**
     * Updates order status validating transition matrix from business rules.
     */
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UUID actorId, UpdateOrderStatusRequest request) {
        Order order = findOrderOrThrow(orderId);
        OrderStatusEnum current = order.getStatus();
        OrderStatusEnum next = request.getStatus();

        if (!isTransitionAllowed(current, next)) {
            throw new BusinessException("Invalid status transition: " + current + " -> " + next);
        }

        order.setStatus(next);
        Order saved = orderRepository.save(order);
        saveStatusHistory(orderId, current, next, normalizeNotes(request.getNotes(), "Status updated"), actorId);

        // Stock confirmation is tied to payment approval in the business flow.
        if (next == OrderStatusEnum.PAYMENT_CONFIRMED) {
            inventoryServiceClient.confirmStock(INTERNAL_CALL_HEADER_VALUE, new InventoryOrderRequest(orderId));
            orderEventProducer.publishOrderPaid(new OrderPaidEvent(UUID.randomUUID(), Instant.now(), orderId));
        }

        if (next == OrderStatusEnum.SHIPPED) {
            orderEventProducer.publishOrderShipped(new OrderShippedEvent(
                UUID.randomUUID(),
                Instant.now(),
                orderId,
                "TRACKING_PENDING"
            ));
        }

        return toResponseWithItems(saved);
    }

    private boolean isTransitionAllowed(OrderStatusEnum current, OrderStatusEnum next) {
        return switch (current) {
            case PENDING_PAYMENT -> next == OrderStatusEnum.PAYMENT_CONFIRMED
                || next == OrderStatusEnum.PAYMENT_FAILED
                || next == OrderStatusEnum.CANCELLED;
            case PAYMENT_CONFIRMED -> next == OrderStatusEnum.PROCESSING
                || next == OrderStatusEnum.CANCELLED
                || next == OrderStatusEnum.REFUNDED;
            case PROCESSING -> next == OrderStatusEnum.SHIPPED
                || next == OrderStatusEnum.CANCELLED
                || next == OrderStatusEnum.REFUNDED;
            case SHIPPED -> next == OrderStatusEnum.DELIVERED;
            default -> false;
        };
    }

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
    }

    private void validateOwnerOrAdmin(Order order, UUID userId, String role) {
        if (!isAdmin(role) && !order.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }

    private OrderResponse toResponseWithItems(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return orderMapper.toResponse(order, items);
    }

    private UserResponse fetchUserOrThrow(UUID userId) {
        try {
            return userServiceClient.getCurrentUser(userId);
        } catch (RuntimeException e) {
            throw new BusinessException("Problem finding user to create an order");
        }
    }

    private AddressResponse fetchAddressOrThrow(UUID userId, UUID addressId) {
        try {
            return userServiceClient.getAddressById(userId, addressId);
        } catch (RuntimeException e) {
            throw new BusinessException("Problem finding a valid address for the user");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException("Problem creating shipping address snapshot");
        }
    }

    /**
     * Validates that all requested products exist and are active.
     */
    private List<ProductResponse> fetchAndValidateActiveProducts(List<OrderItemRequest> items) {
        Set<UUID> requestedIds = items.stream()
            .map(OrderItemRequest::getProductId)
            .collect(Collectors.toSet());

        List<ProductResponse> products = productServiceClient.getAllByListIds(new ArrayList<>(requestedIds));
        Set<UUID> returnedIds = products.stream()
            .map(ProductResponse::id)
            .collect(Collectors.toSet());

        if (returnedIds.size() != requestedIds.size()) {
            Set<UUID> missing = new HashSet<>(requestedIds);
            missing.removeAll(returnedIds);
            throw new BusinessException("Some products are missing or inactive: " + missing);
        }

        return products;
    }

    /**
     * Calls inventory service to reserve stock for all order items.
     */
    private void reserveStock(UUID orderId, List<OrderItemRequest> items) {
        List<InventoryReserveItemRequest> reserveItems = items.stream()
            .map(item -> new InventoryReserveItemRequest(item.getProductId(), item.getQuantity()))
            .toList();

        InventoryReserveRequest reserveRequest = new InventoryReserveRequest(orderId, reserveItems);
        inventoryServiceClient.reserveStock(INTERNAL_CALL_HEADER_VALUE, reserveRequest);
    }

    private void saveStatusHistory(
        UUID orderId,
        OrderStatusEnum fromStatus,
        OrderStatusEnum toStatus,
        String notes,
        UUID createdBy
    ) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setNotes(notes);
        history.setCreatedBy(createdBy);
        orderStatusHistoryRepository.save(history);
    }

    private void publishOrderCreatedEvent(Order order, List<OrderItem> items) {
        List<OrderCreatedItemEvent> eventItems = items.stream()
            .map(item -> new OrderCreatedItemEvent(
                item.getProductId(),
                item.getProductName(),
                item.getProductSku(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
            ))
            .toList();

        orderEventProducer.publishOrderCreated(new OrderCreatedEvent(
            UUID.randomUUID(),
            Instant.now(),
            order.getId(),
            order.getUserId(),
            eventItems,
            order.getTotalAmount()
        ));
    }

    private String normalizeNotes(String notes, String fallback) {
        if (notes == null || notes.isBlank()) {
            return fallback;
        }
        return notes;
    }

    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase();
        return ORDER_NUMBER_PREFIX + "-" + datePart + "-" + randomPart;
    }
}

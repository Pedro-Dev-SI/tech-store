package com.br.orderservice.controller;

import com.br.orderservice.exception.ForbiddenException;
import com.br.orderservice.service.OrderService;
import com.br.orderservice.service.dto.CancelOrderRequest;
import com.br.orderservice.service.dto.CreateOrderRequest;
import com.br.orderservice.service.dto.OrderResponse;
import com.br.orderservice.service.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Creates a new order for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @RequestHeader("X-User-Id") UUID userId,
        @RequestBody @Valid CreateOrderRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId, request));
    }

    /**
     * Returns paginated orders of the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listMyOrders(
        @RequestHeader("X-User-Id") UUID userId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.listMyOrders(userId, pageable));
    }

    /**
     * Returns one order by id if caller is owner or admin.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-User-Role") String role
    ) {
        return ResponseEntity.ok(orderService.getOrderById(id, userId, role));
    }

    /**
     * Cancels an order according to user/admin business rules.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
        @PathVariable UUID id,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-User-Role") String role,
        @RequestBody(required = false) CancelOrderRequest request
    ) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.ok(orderService.cancelOrder(id, userId, role, reason));
    }

    /**
     * Returns all orders for admins.
     */
    @GetMapping("/admin")
    public ResponseEntity<Page<OrderResponse>> listAllOrders(
        @RequestHeader("X-User-Role") String role,
        Pageable pageable
    ) {
        ensureAdmin(role);
        return ResponseEntity.ok(orderService.listAllOrders(pageable));
    }

    /**
     * Updates order status following transition rules (admin only).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
        @PathVariable UUID id,
        @RequestHeader("X-User-Role") String role,
        @RequestHeader("X-User-Id") UUID actorId,
        @RequestBody @Valid UpdateOrderStatusRequest request
    ) {
        ensureAdmin(role);
        return ResponseEntity.ok(orderService.updateOrderStatus(id, actorId, request));
    }

    private void ensureAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Access denied");
        }
    }
}

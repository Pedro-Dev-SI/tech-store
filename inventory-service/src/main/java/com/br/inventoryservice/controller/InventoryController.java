package com.br.inventoryservice.controller;

import com.br.inventoryservice.exception.BusinessException;
import com.br.inventoryservice.service.dto.ConfirmStockRequest;
import com.br.inventoryservice.service.dto.InventoryResponse;
import com.br.inventoryservice.service.dto.ReleaseStockRequest;
import com.br.inventoryservice.service.dto.ReserveStockRequest;
import com.br.inventoryservice.service.dto.UpdateInventoryRequest;
import com.br.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Returns inventory for a product (admin only).
     */
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getByProductId(
        @RequestHeader("X-User-Role") String role,
        @PathVariable UUID productId
    ) {
        ensureAdmin(role);
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.getByProductId(productId));
    }

    /**
     * Returns inventory for a product (internal calls only).
     */
    @GetMapping("/internal/{productId}")
    public ResponseEntity<InventoryResponse> getByProductIdInternal(
        @RequestHeader("X-Internal-Call") String internalCall,
        @PathVariable UUID productId
    ) {
        ensureInternal(internalCall);
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.getByProductId(productId));
    }

    /**
     * Updates inventory quantity and min stock alert (admin only).
     */
    @PutMapping("/{productId}")
    public ResponseEntity<InventoryResponse> updateInventory(
        @PathVariable UUID productId,
        @RequestBody @Valid UpdateInventoryRequest request,
        @RequestHeader("X-User-Role") String role
    ) {
        ensureAdmin(role);
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.updateInventory(productId, request));
    }

    /**
     * Reserves stock for an order (internal only).
     */
    @PostMapping("/reserve")
    public ResponseEntity<List<InventoryResponse>> reserve(
        @RequestBody @Valid ReserveStockRequest request,
        @RequestHeader("X-Internal-Call") String internalCall
    ) {
        ensureInternal(internalCall);
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.reserveStock(request));
    }

    /**
     * Releases reserved stock (internal only).
     */
    @PostMapping("/release")
    public ResponseEntity<List<InventoryResponse>> release(
        @RequestBody @Valid ReleaseStockRequest request,
        @RequestHeader("X-Internal-Call") String internalCall
    ) {
        ensureInternal(internalCall);
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.releaseStock(request));
    }

    /**
     * Confirms stock consumption (internal only).
     */
    @PostMapping("/confirm")
    public ResponseEntity<List<InventoryResponse>> confirm(
        @RequestBody @Valid ConfirmStockRequest request,
        @RequestHeader("X-Internal-Call") String internalCall
    ) {
        ensureInternal(internalCall);
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.confirmStock(request));
    }

    /**
     * Lists products with low stock (admin only).
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> lowStock(@RequestHeader("X-User-Role") String role) {
        ensureAdmin(role);
        return ResponseEntity.status(HttpStatus.OK).body(inventoryService.listLowStock());
    }

    private void ensureAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new BusinessException("Access denied");
        }
    }

    private void ensureInternal(String internalCall) {
        if (!"true".equalsIgnoreCase(internalCall)) {
            throw new BusinessException("Access denied");
        }
    }
}

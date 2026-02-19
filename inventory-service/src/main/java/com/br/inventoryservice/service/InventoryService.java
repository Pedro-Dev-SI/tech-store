package com.br.inventoryservice.service;

import com.br.inventoryservice.event.InventoryEventProducer;
import com.br.inventoryservice.event.StockConfirmedEvent;
import com.br.inventoryservice.event.StockLowAlertEvent;
import com.br.inventoryservice.event.StockReleasedEvent;
import com.br.inventoryservice.event.StockReservedEvent;
import com.br.inventoryservice.service.dto.ConfirmStockRequest;
import com.br.inventoryservice.service.dto.InventoryResponse;
import com.br.inventoryservice.service.dto.ReleaseStockRequest;
import com.br.inventoryservice.service.dto.ReserveStockRequest;
import com.br.inventoryservice.service.dto.StockItemRequest;
import com.br.inventoryservice.service.dto.UpdateInventoryRequest;
import com.br.inventoryservice.enums.StockMovementType;
import com.br.inventoryservice.exception.BusinessException;
import com.br.inventoryservice.exception.ResourceNotFoundException;
import com.br.inventoryservice.model.Inventory;
import com.br.inventoryservice.model.StockMovement;
import com.br.inventoryservice.repository.InventoryRepository;
import com.br.inventoryservice.repository.StockMovementRepository;
import com.br.inventoryservice.service.mapper.InventoryMapper;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryEventProducer inventoryEventProducer;

    public InventoryService(
        InventoryRepository inventoryRepository,
        StockMovementRepository stockMovementRepository,
        InventoryMapper inventoryMapper,
        InventoryEventProducer inventoryEventProducer
    ) {
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.inventoryMapper = inventoryMapper;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory", productId.toString()));
        return inventoryMapper.toResponse(inventory);
    }

    @Transactional
    public InventoryResponse updateInventory(UUID productId, @Valid UpdateInventoryRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory", productId.toString()));

        inventory.setQuantity(request.getQuantity());
        inventory.setMinStockAlert(request.getMinStockAlert());

        Inventory saved = inventoryRepository.save(inventory);
        publishLowStockIfNeeded(saved);
        return inventoryMapper.toResponse(saved);
    }

    @Transactional
    public List<InventoryResponse> reserveStock(@Valid ReserveStockRequest request) {
        List<Inventory> updated = new ArrayList<>();

        for (StockItemRequest item : request.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", item.getProductId().toString()));

            int available = inventory.getQuantity() - inventory.getReservedQuantity();
            if (available < item.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + item.getProductId());
            }

            inventory.setReservedQuantity(inventory.getReservedQuantity() + item.getQuantity());
            updated.add(inventory);
        }

        for (int i = 0; i < request.getItems().size(); i++) {
            Inventory inventory = updated.get(i);
            StockItemRequest item = request.getItems().get(i);

            StockMovement movement = StockMovement.builder()
                .inventory(inventory)
                .type(StockMovementType.RESERVE)
                .quantity(item.getQuantity())
                .reason("Reserve stock")
                .orderId(request.getOrderId())
                .build();

            stockMovementRepository.save(movement);
            inventoryRepository.save(inventory);
            inventoryEventProducer.publishStockReserved(new StockReservedEvent(
                UUID.randomUUID(),
                Instant.now(),
                request.getOrderId(),
                item.getProductId(),
                item.getQuantity()
            ));
        }

        return updated.stream().map(inventoryMapper::toResponse).toList();
    }

    @Transactional
    public List<InventoryResponse> releaseStock(@Valid ReleaseStockRequest request) {
        List<StockMovement> reserves = stockMovementRepository.findByOrderIdAndType(request.getOrderId(), StockMovementType.RESERVE);
        if (reserves.isEmpty()) {
            throw new BusinessException("No reserved stock found for order: " + request.getOrderId());
        }

        List<InventoryResponse> result = new ArrayList<>();
        for (StockMovement reserve : reserves) {
            Inventory inventory = reserve.getInventory();
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reserve.getQuantity());

            StockMovement release = StockMovement.builder()
                .inventory(inventory)
                .type(StockMovementType.RELEASE)
                .quantity(reserve.getQuantity())
                .reason("Release reservation")
                .orderId(request.getOrderId())
                .build();

            stockMovementRepository.save(release);
            inventoryRepository.save(inventory);
            inventoryEventProducer.publishStockReleased(new StockReleasedEvent(
                UUID.randomUUID(),
                Instant.now(),
                request.getOrderId(),
                reserve.getInventory().getProductId(),
                reserve.getQuantity()
            ));
            result.add(inventoryMapper.toResponse(inventory));
        }

        return result;
    }

    @Transactional
    public List<InventoryResponse> confirmStock(@Valid ConfirmStockRequest request) {
        List<StockMovement> reserves = stockMovementRepository.findByOrderIdAndType(request.getOrderId(), StockMovementType.RESERVE);
        if (reserves.isEmpty()) {
            throw new BusinessException("No reserved stock found for order: " + request.getOrderId());
        }

        List<InventoryResponse> result = new ArrayList<>();
        for (StockMovement reserve : reserves) {
            Inventory inventory = reserve.getInventory();
            inventory.setQuantity(inventory.getQuantity() - reserve.getQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reserve.getQuantity());

            StockMovement out = StockMovement.builder()
                .inventory(inventory)
                .type(StockMovementType.OUT)
                .quantity(reserve.getQuantity())
                .reason("Confirm stock output")
                .orderId(request.getOrderId())
                .build();

            stockMovementRepository.save(out);
            inventoryRepository.save(inventory);
            inventoryEventProducer.publishStockConfirmed(new StockConfirmedEvent(
                UUID.randomUUID(),
                Instant.now(),
                request.getOrderId(),
                reserve.getInventory().getProductId(),
                reserve.getQuantity()
            ));
            publishLowStockIfNeeded(inventory);
            result.add(inventoryMapper.toResponse(inventory));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> listLowStock() {
        return inventoryRepository.findLowStock().stream().map(inventoryMapper::toResponse).toList();
    }

    private void publishLowStockIfNeeded(Inventory inventory) {
        if (inventory.getQuantity() <= inventory.getMinStockAlert()) {
            inventoryEventProducer.publishStockLowAlert(new StockLowAlertEvent(
                UUID.randomUUID(),
                Instant.now(),
                inventory.getProductId(),
                inventory.getQuantity(),
                inventory.getMinStockAlert()
            ));
        }
    }
}

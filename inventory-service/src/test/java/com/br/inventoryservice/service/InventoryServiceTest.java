package com.br.inventoryservice.service;

import com.br.inventoryservice.enums.StockMovementType;
import com.br.inventoryservice.exception.BusinessException;
import com.br.inventoryservice.exception.ResourceNotFoundException;
import com.br.inventoryservice.model.Inventory;
import com.br.inventoryservice.model.StockMovement;
import com.br.inventoryservice.repository.InventoryRepository;
import com.br.inventoryservice.repository.StockMovementRepository;
import com.br.inventoryservice.service.dto.ReserveStockRequest;
import com.br.inventoryservice.service.dto.StockItemRequest;
import com.br.inventoryservice.service.dto.UpdateInventoryRequest;
import com.br.inventoryservice.service.mapper.InventoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void getByProductId_notFound_throws() {
        UUID productId = UUID.randomUUID();
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventoryService.getByProductId(productId));
    }

    @Test
    void updateInventory_updatesQuantityAndMinStock() {
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
            .id(UUID.randomUUID())
            .productId(productId)
            .quantity(5)
            .reservedQuantity(1)
            .minStockAlert(2)
            .updatedAt(LocalDateTime.now())
            .build();

        UpdateInventoryRequest request = new UpdateInventoryRequest();
        request.setQuantity(10);
        request.setMinStockAlert(3);

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.updateInventory(productId, request);

        assertEquals(10, inventory.getQuantity());
        assertEquals(3, inventory.getMinStockAlert());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserveStock_insufficient_throws() {
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
            .id(UUID.randomUUID())
            .productId(productId)
            .quantity(2)
            .reservedQuantity(2)
            .minStockAlert(1)
            .build();

        ReserveStockRequest request = new ReserveStockRequest();
        request.setOrderId(UUID.randomUUID());
        StockItemRequest item = new StockItemRequest();
        item.setProductId(productId);
        item.setQuantity(1);
        request.setItems(List.of(item));

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        assertThrows(BusinessException.class, () -> inventoryService.reserveStock(request));
    }

    @Test
    void reserveStock_success_updatesReserved() {
        UUID productId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
            .id(UUID.randomUUID())
            .productId(productId)
            .quantity(10)
            .reservedQuantity(1)
            .minStockAlert(1)
            .build();

        ReserveStockRequest request = new ReserveStockRequest();
        request.setOrderId(UUID.randomUUID());
        StockItemRequest item = new StockItemRequest();
        item.setProductId(productId);
        item.setQuantity(2);
        request.setItems(List.of(item));

        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.reserveStock(request);

        assertEquals(3, inventory.getReservedQuantity());
    }

    @Test
    void releaseStock_noReserve_throws() {
        when(stockMovementRepository.findByOrderIdAndType(any(), any())).thenReturn(List.of());

        assertThrows(BusinessException.class, () ->
            inventoryService.releaseStock(new com.br.inventoryservice.service.dto.ReleaseStockRequest() {{
                setOrderId(UUID.randomUUID());
            }})
        );
    }

    @Test
    void confirmStock_noReserve_throws() {
        when(stockMovementRepository.findByOrderIdAndType(any(), any())).thenReturn(List.of());

        assertThrows(BusinessException.class, () ->
            inventoryService.confirmStock(new com.br.inventoryservice.service.dto.ConfirmStockRequest() {{
                setOrderId(UUID.randomUUID());
            }})
        );
    }

    @Test
    void releaseStock_success_updatesReserved() {
        UUID orderId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
            .id(UUID.randomUUID())
            .productId(UUID.randomUUID())
            .quantity(10)
            .reservedQuantity(5)
            .minStockAlert(1)
            .build();

        StockMovement reserve = StockMovement.builder()
            .inventory(inventory)
            .type(StockMovementType.RESERVE)
            .quantity(2)
            .orderId(orderId)
            .build();

        when(stockMovementRepository.findByOrderIdAndType(orderId, StockMovementType.RESERVE))
            .thenReturn(List.of(reserve));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.releaseStock(new com.br.inventoryservice.service.dto.ReleaseStockRequest() {{
            setOrderId(orderId);
        }});

        assertEquals(3, inventory.getReservedQuantity());
    }

    @Test
    void confirmStock_success_updatesQuantityAndReserved() {
        UUID orderId = UUID.randomUUID();
        Inventory inventory = Inventory.builder()
            .id(UUID.randomUUID())
            .productId(UUID.randomUUID())
            .quantity(10)
            .reservedQuantity(4)
            .minStockAlert(1)
            .build();

        StockMovement reserve = StockMovement.builder()
            .inventory(inventory)
            .type(StockMovementType.RESERVE)
            .quantity(2)
            .orderId(orderId)
            .build();

        when(stockMovementRepository.findByOrderIdAndType(orderId, StockMovementType.RESERVE))
            .thenReturn(List.of(reserve));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

        inventoryService.confirmStock(new com.br.inventoryservice.service.dto.ConfirmStockRequest() {{
            setOrderId(orderId);
        }});

        assertEquals(8, inventory.getQuantity());
        assertEquals(2, inventory.getReservedQuantity());
    }
}

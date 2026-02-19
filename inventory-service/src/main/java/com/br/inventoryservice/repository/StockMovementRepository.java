package com.br.inventoryservice.repository;

import com.br.inventoryservice.enums.StockMovementType;
import com.br.inventoryservice.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByOrderIdAndType(UUID orderId, StockMovementType type);
}

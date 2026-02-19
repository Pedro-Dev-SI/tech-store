package com.br.inventoryservice.service.mapper;

import com.br.inventoryservice.model.StockMovement;
import com.br.inventoryservice.service.dto.StockMovementResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    @Mapping(target = "inventoryId", source = "inventory.id")
    StockMovementResponse toResponse(StockMovement movement);
}

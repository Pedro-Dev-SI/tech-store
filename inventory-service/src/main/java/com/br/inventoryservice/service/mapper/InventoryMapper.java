package com.br.inventoryservice.service.mapper;

import com.br.inventoryservice.model.Inventory;
import com.br.inventoryservice.service.dto.InventoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryMapper {


    InventoryResponse toResponse(Inventory inventory);
}

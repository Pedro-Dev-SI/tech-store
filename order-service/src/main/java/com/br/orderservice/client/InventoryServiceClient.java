package com.br.orderservice.client;

import com.br.orderservice.service.dto.InventoryOrderRequest;
import com.br.orderservice.service.dto.InventoryReserveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "inventory-service", url = "${inventory-service.base-url}")
public interface InventoryServiceClient {

    @PostMapping("/api/v1/inventory/reserve")
    void reserveStock(
        @RequestHeader("X-Internal-Call") String internalCall,
        @RequestBody InventoryReserveRequest request
    );

    @PostMapping("/api/v1/inventory/release")
    void releaseStock(
        @RequestHeader("X-Internal-Call") String internalCall,
        @RequestBody InventoryOrderRequest request
    );

    @PostMapping("/api/v1/inventory/confirm")
    void confirmStock(
        @RequestHeader("X-Internal-Call") String internalCall,
        @RequestBody InventoryOrderRequest request
    );
}

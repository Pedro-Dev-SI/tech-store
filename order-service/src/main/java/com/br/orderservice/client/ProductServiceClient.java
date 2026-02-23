package com.br.orderservice.client;

import com.br.orderservice.service.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "product-service", url = "${product-service.base-url}")
public interface ProductServiceClient {

    @PostMapping("/api/v1/products/list/all")
    List<ProductResponse> getAllByListIds(@RequestBody List<UUID> ids);

}

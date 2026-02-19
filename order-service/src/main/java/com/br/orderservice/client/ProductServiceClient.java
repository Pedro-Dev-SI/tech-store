package com.br.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "product-service", url = "${product-service.base-url}")
public interface ProductServiceClient {


}

package com.br.orderservice.client;

import com.br.orderservice.service.dto.AddressResponse;
import com.br.orderservice.service.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "user-service", url = "${user-service.base-url}")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/me")
    UserResponse getCurrentUser(@RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/api/v1/users/me/addresses")
    List<AddressResponse> getUserAddresses(@RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/api/v1/users/me/addresses/default")
    AddressResponse getDefaultAddress(@RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/api/v1/users/me/addresses/{id}")
    AddressResponse getAddressById(@PathVariable UUID id);
}

package com.br.authservice.client;

import com.br.authservice.service.dto.LoginRequest;
import com.br.authservice.service.dto.RegisterRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "user-service", url = "${user-service.base-url}")
public interface UserServiceClient {

    @PostMapping("/api/v1/users")
    UserResponse createUser(@RequestBody RegisterRequest request);

    @PostMapping("/api/v1/users/login")
    LoginResponse validateLogin(@RequestBody LoginRequest request);

    record UserResponse(UUID id, String email, String role) {}

    record LoginResponse(UUID id, String email, String role, String status) {}
}

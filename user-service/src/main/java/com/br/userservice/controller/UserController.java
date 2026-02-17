package com.br.userservice.controller;

import com.br.userservice.enums.RoleEnum;
import com.br.userservice.service.UserService;
import com.br.userservice.service.dto.CreateUserDTO;
import com.br.userservice.service.dto.LoginRequest;
import com.br.userservice.service.dto.LoginResponse;
import com.br.userservice.service.dto.UpdateUserDTO;
import com.br.userservice.service.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     *
     * @param createUserDTO
     * @return
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserDTO createUserDTO) {
        log.info("REST - Request to save a new user whit the name: {}", createUserDTO.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(createUserDTO));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("REST - Request to validate login for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(userService.validateLogin(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("X-User-Id") UUID userId){
        log.info("REST - Request to get current user wiht id: {}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(userService.findById(userId));
    }

    @PatchMapping("")
    public ResponseEntity<UserResponse> updateUser(@RequestHeader("X-User-Id") UUID id, @RequestBody UpdateUserDTO updateUserDTO) {
        log.info("REST - Request to update a user with id: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(id, updateUserDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") UUID userId, @RequestHeader("X-User-Role") String role) {
        log.info("REST - Request to get a user with id: {}", userId);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userService.findById(userId));
    }

    @GetMapping
    public ResponseEntity<?> listAllUsers(
        @RequestHeader("X-User-Role") String role,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        log.info("REST - Request to list all users");
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        return ResponseEntity.status(HttpStatus.OK).body(userService.listAllUsers(page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable UUID id, @RequestHeader("X-User-Role") String role) {
        log.info("REST - Request to deactivate a user with id: {}", id);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        userService.deactivateUser(id);
        return ResponseEntity.status(HttpStatus.OK).body("User has been deactivated successfully");
    }

    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<?> blockUser(@PathVariable UUID id, @RequestHeader("X-User-Role") String role) {
        log.info("REST - Request to block user with id: {}", id);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        userService.blockUser(id);
        return ResponseEntity.status(HttpStatus.OK).body("User has been blocked successfully");
    }



}

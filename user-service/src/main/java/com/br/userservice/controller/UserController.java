package com.br.userservice.controller;

import com.br.userservice.enums.RoleEnum;
import com.br.userservice.service.AddressService;
import com.br.userservice.service.UserService;
import com.br.userservice.service.dto.AddressResponse;
import com.br.userservice.service.dto.CreateAddressDTO;
import com.br.userservice.service.dto.CreateUserDTO;
import com.br.userservice.service.dto.LoginRequest;
import com.br.userservice.service.dto.LoginResponse;
import com.br.userservice.service.dto.UpdateAddressDTO;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    public UserController(UserService userService, AddressService addressService) {
        this.userService = userService;
        this.addressService = addressService;
    }

    /**
     * Creates a new user (public endpoint).
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserDTO createUserDTO) {
        log.info("REST - Request to save a new user whit the name: {}", createUserDTO.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(createUserDTO));
    }

    /**
     * Validates user credentials (internal use by auth-service).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("REST - Request to validate login for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(userService.validateLogin(request));
    }

    /**
     * Returns the current authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("X-User-Id") UUID userId){
        log.info("REST - Request to get current user wiht id: {}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(userService.findById(userId));
    }

    /**
     * Partially updates the current authenticated user (name/phone).
     */
    @PatchMapping("")
    public ResponseEntity<UserResponse> updateUser(@RequestHeader("X-User-Id") UUID id, @RequestBody UpdateUserDTO updateUserDTO) {
        log.info("REST - Request to update a user with id: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(id, updateUserDTO));
    }

    /**
     * Returns a user by id (admin only).
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") UUID userId, @RequestHeader("X-User-Role") String role) {
        log.info("REST - Request to get a user with id: {}", userId);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.status(HttpStatus.OK).body(userService.findById(userId));
    }

    /**
     * Returns a paginated list of users (admin only).
     */
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

    /**
     * Deactivates a user (soft delete) - admin only.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable UUID id, @RequestHeader("X-User-Role") String role) {
        log.info("REST - Request to deactivate a user with id: {}", id);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        userService.deactivateUser(id);
        return ResponseEntity.status(HttpStatus.OK).body("User has been deactivated successfully");
    }

    /**
     * Blocks a user (admin only).
     */
    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<?> blockUser(@PathVariable UUID id, @RequestHeader("X-User-Role") String role) {
        log.info("REST - Request to block user with id: {}", id);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        userService.blockUser(id);
        return ResponseEntity.status(HttpStatus.OK).body("User has been blocked successfully");
    }

    /**
     * Lists all addresses for the authenticated user.
     */
    @GetMapping("/me/addresses")
    public ResponseEntity<List<AddressResponse>> listAllUserAddresses(@RequestHeader("X-User-Id") UUID userId) {
        log.info("REST - Request to list all user addresses with user id: {}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(addressService.getAllUserAddresses(userId));
    }

    @GetMapping("/me/addresses/{id}")
    public ResponseEntity<AddressResponse> getAddressById(@PathVariable UUID id) {
        log.info("REST - Request to get an address by id: {}", id);
        return ResponseEntity.status(HttpStatus.OK).body(addressService.findById(id));
    }

    @GetMapping("/me/addresses/default")
    public ResponseEntity<AddressResponse> getDefaultAddress(@RequestHeader("X-User-Id") UUID userId) {
        log.info("REST - Request to get the default address by user id: {}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(addressService.getDefaultAddress(userId));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<AddressResponse> addAddress(@RequestHeader("X-User-Id") UUID userId, @RequestBody @Valid CreateAddressDTO createAddressDTO){
        log.info("REST - Request to add new address to the user with the id: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.addNewAddress(createAddressDTO, userId));
    }

    @PatchMapping("/me/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID addressId,
        @RequestBody @Valid UpdateAddressDTO updateAddressDTO
    ) {
        log.info("REST - Request to update a address from a user with the id: {}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(addressService.updateAddress(updateAddressDTO, userId, addressId));
    }

    /**
     * Deletes an address by id (authenticated user only).
     */
    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID addressId
    ) {
        log.info("REST - Request to delete address {} for user {}", addressId, userId);
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

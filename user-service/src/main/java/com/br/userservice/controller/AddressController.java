package com.br.userservice.controller;

import com.br.userservice.service.AddressService;
import com.br.userservice.service.dto.AddressResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * Lists all addresses for the authenticated user.
     */
    @GetMapping("/list-all-user-adresses")
    public ResponseEntity<List<AddressResponse>> listAllUserAddresses(@RequestHeader("X-User-Id") UUID userId) {
        log.info("REST - Request to list all user addresses with user id: {}", userId);
        return ResponseEntity.status(HttpStatus.OK).body(addressService.getAllUserAddresses(userId));
    }
}

package com.br.userservice.controller;

import com.br.userservice.service.UserService;
import com.br.userservice.service.dto.CreateUserDTO;
import com.br.userservice.service.dto.LoginRequest;
import com.br.userservice.service.dto.LoginResponse;
import com.br.userservice.service.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}

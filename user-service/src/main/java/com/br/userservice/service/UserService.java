package com.br.userservice.service;

import com.br.userservice.enums.RoleEnum;
import com.br.userservice.enums.StatusEnum;
import com.br.userservice.exception.BusinessException;
import com.br.userservice.exception.ResourceNotFoundException;
import com.br.userservice.model.User;
import com.br.userservice.model.vo.Cpf;
import com.br.userservice.model.vo.Email;
import com.br.userservice.model.vo.Phone;
import com.br.userservice.repository.UserRepository;
import com.br.userservice.service.dto.CreateUserDTO;
import com.br.userservice.service.dto.LoginRequest;
import com.br.userservice.service.dto.LoginResponse;
import com.br.userservice.service.dto.UpdateUserDTO;
import com.br.userservice.service.dto.UserResponse;
import com.br.userservice.service.mapper.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


/**
 * Business logic for user management.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    /**
     * Creates a new user with validation and password hashing.
     */
    @Transactional
    public UserResponse createUser(CreateUserDTO createUserDTO) {

        Email email = Email.of(createUserDTO.getEmail());
        Cpf cpf = Cpf.of(createUserDTO.getCpf());
        Phone phone = Phone.of(createUserDTO.getPhone());

        boolean isEmailRegistered = userRepository.existsByEmail(email);
        if (isEmailRegistered) {
            throw new BusinessException("Email informed has already been registered");
        }

        boolean isCpfRegistered = userRepository.existsByCpf(cpf);
        if (isCpfRegistered) {
            throw new BusinessException("CPF informed has already been registered");
        }

        boolean isPhoneRegistered = userRepository.existsByPhone(phone);
        if (isPhoneRegistered) {
            throw new BusinessException("Phone informed has already been registered");
        }

        String hashedPassword = passwordEncoder.encode(createUserDTO.getPassword());

        //Create a new user
        User newUser = new User();
        newUser.setName(createUserDTO.getName());
        newUser.setCpf(cpf);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setPassword(hashedPassword);
        newUser.setRole(RoleEnum.USER);
        newUser.setStatus(StatusEnum.ACTIVE);

        User savedUser = userRepository.save(newUser);

        return userMapper.toResponse(savedUser);
    }

    /**
     * Validates user credentials and returns minimal auth data.
     */
    @Transactional(readOnly = true)
    public LoginResponse validateLogin(LoginRequest request) {
        Email email = Email.of(request.getEmail());
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid credentials");
        }

        return new LoginResponse(
            user.getId(),
            user.getEmail().getValue(),
            user.getRole().name(),
            user.getStatus().name()
        );
    }

    /**
     * Returns a user by id.
     */
    @Transactional
    public UserResponse findById(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        return userMapper.toResponse(user);
    }

    /**
     * Checks if a user exists by id.
     */
    public Boolean existsById(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }
        return userRepository.existsById(userId);
    }

    /**
     * Updates user name and/or phone.
     */
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserDTO updateUserDTO) {

        if (id == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }

        if (updateUserDTO == null) {
            throw new IllegalArgumentException("User data cannot be null");
        }

        User userToUpdate = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

        if (updateUserDTO.getName() != null && updateUserDTO.getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }

        Phone phone = updateUserDTO.getPhone() != null ? Phone.of(updateUserDTO.getPhone()) : null;

        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BusinessException("Phone informed has already been registered");
        }

        userToUpdate.setName(updateUserDTO.getName() != null ? updateUserDTO.getName() : userToUpdate.getName());
        userToUpdate.setPhone(phone != null ? phone : userToUpdate.getPhone());

        User userUpdated = userRepository.save(userToUpdate);

        return userMapper.toResponse(userUpdated);
    }

    /**
     * Returns paginated users (admin only at controller level).
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> listAllUsers(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    /**
     * Soft-deletes a user by setting status to INACTIVE.
     */
    public void deactivateUser(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        User userToBeDeactivated = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

        userToBeDeactivated.setStatus(StatusEnum.INACTIVE);

    }

    /**
     * Blocks a user by setting status to BLOCKED.
     */
    public void blockUser(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        User userToBeDeactivated = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

        userToBeDeactivated.setStatus(StatusEnum.BLOCKED);
    }
}

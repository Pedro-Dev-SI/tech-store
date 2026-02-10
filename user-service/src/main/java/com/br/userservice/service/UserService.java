package com.br.userservice.service;

import com.br.userservice.enums.RoleEnum;
import com.br.userservice.enums.StatusEnum;
import com.br.userservice.exception.BusinessException;
import com.br.userservice.model.User;
import com.br.userservice.model.vo.Cpf;
import com.br.userservice.model.vo.Email;
import com.br.userservice.model.vo.Phone;
import com.br.userservice.repository.UserRepository;
import com.br.userservice.service.dto.CreateUserDTO;
import com.br.userservice.service.dto.UserResponse;
import com.br.userservice.service.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
     *
     * @param createUserDTO
     * @return
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
}

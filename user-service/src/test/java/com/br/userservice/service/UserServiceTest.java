package com.br.userservice.service;

import com.br.userservice.enums.RoleEnum;
import com.br.userservice.enums.StatusEnum;
import com.br.userservice.exception.BusinessException;
import com.br.userservice.model.User;
import com.br.userservice.repository.UserRepository;
import com.br.userservice.service.dto.CreateUserDTO;
import com.br.userservice.service.dto.UserResponse;
import com.br.userservice.service.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private CreateUserDTO buildCreateUserDTO() {
        return new CreateUserDTO(
            "teste@exemplo.com",
            "52998224725",
            "11999999999",
            "Pedro",
            "SenhaForte1"
        );
    }

    @Test
    void createUser_success_persistsWithHashedPasswordAndDefaults() {
        CreateUserDTO dto = buildCreateUserDTO();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByCpf(any())).thenReturn(false);
        when(userRepository.existsByPhone(any())).thenReturn(false);
        when(passwordEncoder.encode("SenhaForte1")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse(
            UUID.randomUUID(),
            dto.getEmail().toLowerCase(),
            dto.getCpf(),
            dto.getPhone(),
            dto.getName(),
            RoleEnum.USER.name(),
            StatusEnum.ACTIVE.name()
        ));

        userService.createUser(dto);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals("hash", saved.getPassword());
        assertEquals(RoleEnum.USER, saved.getRole());
        assertEquals(StatusEnum.ACTIVE, saved.getStatus());
        assertNotNull(saved.getEmail());
        assertNotNull(saved.getCpf());
        assertNotNull(saved.getPhone());
        assertEquals("teste@exemplo.com", saved.getEmail().getValue());
        assertEquals("52998224725", saved.getCpf().getValue());
        assertEquals("11999999999", saved.getPhone().getValue());
    }

    @Test
    void createUser_duplicateEmail_throws() {
        CreateUserDTO dto = buildCreateUserDTO();

        when(userRepository.existsByEmail(any())).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(dto));
    }

    @Test
    void createUser_duplicateCpf_throws() {
        CreateUserDTO dto = buildCreateUserDTO();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByCpf(any())).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(dto));
    }

    @Test
    void createUser_duplicatePhone_throws() {
        CreateUserDTO dto = buildCreateUserDTO();

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByCpf(any())).thenReturn(false);
        when(userRepository.existsByPhone(any())).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(dto));
    }

    @Test
    void createUser_invalidEmail_throws() {
        CreateUserDTO dto = buildCreateUserDTO();
        dto.setEmail("email-invalido");

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(dto));
    }

    @Test
    void createUser_invalidCpf_throws() {
        CreateUserDTO dto = buildCreateUserDTO();
        dto.setCpf("12345678900");

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(dto));
    }

    @Test
    void createUser_invalidPhone_throws() {
        CreateUserDTO dto = buildCreateUserDTO();
        dto.setPhone("123");

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(dto));
    }
}

package com.br.userservice.service.mapper;

import com.br.userservice.model.User;
import com.br.userservice.service.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        String email = user.getEmail() != null ? user.getEmail().getValue() : null;
        String cpf = user.getCpf() != null ? user.getCpf().getValue() : null;
        String phone = user.getPhone() != null ? user.getPhone().getValue() : null;
        String role = user.getRole() != null ? user.getRole().name() : null;
        String status = user.getStatus() != null ? user.getStatus().name() : null;

        return new UserResponse(
            user.getId(),
            email,
            cpf,
            phone,
            user.getName(),
            role,
            status
        );
    }
}

package com.br.userservice.repository;

import com.br.userservice.model.User;
import com.br.userservice.model.vo.Cpf;
import com.br.userservice.model.vo.Email;
import com.br.userservice.model.vo.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Boolean existsByEmail(Email email);

    Boolean existsByCpf(Cpf cpf);

    Boolean existsByPhone(Phone phone);

    Optional<User> findByEmail(Email email);
}

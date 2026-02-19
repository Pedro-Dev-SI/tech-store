package com.br.userservice.repository;

import com.br.userservice.model.Address;
import com.br.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findAllByUserId(UUID userId);

    UUID user(User user);

    Optional<Address> findAddressByUserIdAndIsDefaultIsTrue(UUID userId);
}

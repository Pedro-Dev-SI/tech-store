package com.br.userservice.service;

import com.br.userservice.exception.ResourceNotFoundException;
import com.br.userservice.model.Address;
import com.br.userservice.model.User;
import com.br.userservice.repository.AddressRepository;
import com.br.userservice.service.dto.AddressResponse;
import com.br.userservice.service.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserService userService;
    private final AddressMapper addressMapper;

    public AddressService(AddressRepository addressRepository, UserService userService, AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.userService = userService;
        this.addressMapper = addressMapper;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAllUserAddresses(UUID userId) {

        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        if (!userService.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId.toString());
        }

        List<Address> listOfAddresses = addressRepository.findAllByUserId(userId);

        return listOfAddresses.stream().map(addressMapper::toResponse).collect(Collectors.toList());

    }
}

package com.br.userservice.service;

import com.br.userservice.exception.BusinessException;
import com.br.userservice.exception.ResourceNotFoundException;
import com.br.userservice.model.Address;
import com.br.userservice.model.User;
import com.br.userservice.repository.AddressRepository;
import com.br.userservice.service.dto.AddressResponse;
import com.br.userservice.service.dto.CreateAddressDTO;
import com.br.userservice.service.dto.UpdateAddressDTO;
import com.br.userservice.service.mapper.AddressMapper;
import jakarta.validation.Valid;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Business logic for user addresses.
 */
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

    /**
     * Returns all addresses for a given user.
     */
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

    @Transactional
    public AddressResponse addNewAddress(CreateAddressDTO createAddressDTO, UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        User user = userService.findUserEntityById(userId);

        if (user.getAddressList().size() == 5) {
            throw new BusinessException("Cannot add more than 5 addresses to the user: " + user.getId());
        }

        Address newAddress = getNewAddress(createAddressDTO, user);

        Address addressSaved = addressRepository.save(newAddress);

        return addressMapper.toResponse(addressSaved);


    }

    private static @NonNull Address getNewAddress(CreateAddressDTO createAddressDTO, User user) {
        Address newAddress = new Address();
        newAddress.setStreet(createAddressDTO.getStreet());
        newAddress.setNumber(createAddressDTO.getNumber());
        newAddress.setComplement(createAddressDTO.getComplement());
        newAddress.setNeighborhood(createAddressDTO.getNeighborhood());
        newAddress.setCity(createAddressDTO.getCity());
        newAddress.setState(createAddressDTO.getState());
        newAddress.setZipCode(createAddressDTO.getZiCode());

        if (createAddressDTO.getIsDefault()){
            newAddress.setIsDefault(true);
        }

        newAddress.setIsDefault(user.getAddressList().isEmpty());
        newAddress.setUser(user);
        return newAddress;
    }

    public AddressResponse updateAddress(@Valid UpdateAddressDTO updateAddressDTO, UUID userId, UUID addressId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        if (addressId == null) {
            throw new IllegalArgumentException("Address id must not be null");
        }

        User user = userService.findUserEntityById(userId);

        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", addressId.toString()));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Address does not belong to the user");
        }

        applyIfNotNull(updateAddressDTO.getStreet(), address::setStreet);
        applyIfNotNull(updateAddressDTO.getNumber(), address::setNumber);
        applyIfNotNull(updateAddressDTO.getComplement(), address::setComplement);
        applyIfNotNull(updateAddressDTO.getNeighborhood(), address::setNeighborhood);
        applyIfNotNull(updateAddressDTO.getCity(), address::setCity);
        applyIfNotNull(updateAddressDTO.getState(), address::setState);
        applyIfNotNull(updateAddressDTO.getZipCode(), address::setZipCode);
        applyIfNotNull(updateAddressDTO.getIsDefault(), address::setIsDefault);

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    /**
     * Deletes an address by id after verifying ownership.
     */
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        if (addressId == null) {
            throw new IllegalArgumentException("Address id must not be null");
        }

        User user = userService.findUserEntityById(userId);
        Address address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("Address", addressId.toString()));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new BusinessException("Address does not belong to the user");
        }

        addressRepository.delete(address);
    }

    private <T> void applyIfNotNull(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }

        if (!userService.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId.toString());
        }

        Address defaultAddress = addressRepository.findAddressByUserIdAndIsDefaultIsTrue(userId).orElseThrow(() -> new ResourceNotFoundException("Address", userId.toString()));

        return addressMapper.toResponse(defaultAddress);
    }

    @Transactional(readOnly = true)
    public AddressResponse findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Address id must not be null");
        }

        Address address = addressRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Address", id.toString()));

        return addressMapper.toResponse(address);
    }
}

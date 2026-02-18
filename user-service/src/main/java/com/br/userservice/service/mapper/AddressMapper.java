package com.br.userservice.service.mapper;

import com.br.userservice.model.Address;
import com.br.userservice.service.dto.AddressResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "isDefault", expression = "java(Boolean.TRUE.equals(address.getIsDefault()))")
    AddressResponse toResponse(Address address);
}

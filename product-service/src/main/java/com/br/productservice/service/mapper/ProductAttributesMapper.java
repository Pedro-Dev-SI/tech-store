package com.br.productservice.service.mapper;

import com.br.productservice.model.ProductAttribute;
import com.br.productservice.service.dto.ProductAttributesResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductAttributesMapper {

    @Mapping(target = "productId", source = "product.id")
    ProductAttributesResponse toResponse(ProductAttribute productAttribute);
}

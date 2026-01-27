package com.br.productservice.service.mapper;

import com.br.productservice.model.ProductImage;
import com.br.productservice.service.dto.ProductImageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    @Mapping(target = "productId", source = "product.id")
    ProductImageResponse toResponse(ProductImage productImage);
}

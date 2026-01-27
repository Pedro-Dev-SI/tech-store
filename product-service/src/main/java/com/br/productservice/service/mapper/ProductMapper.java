package com.br.productservice.service.mapper;

import com.br.productservice.model.Product;
import com.br.productservice.service.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "active", expression = "java(Boolean.TRUE.equals(product.getActive()))")
    ProductResponse toResponse(Product product);
}

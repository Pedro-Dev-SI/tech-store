package com.br.productservice.service.mapper;

import com.br.productservice.model.Category;
import com.br.productservice.service.dto.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "active", expression = "java(Boolean.TRUE.equals(category.getActive()))")
    CategoryResponse toResponse(Category category);
}

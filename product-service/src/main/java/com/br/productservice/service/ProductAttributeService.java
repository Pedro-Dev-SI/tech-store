package com.br.productservice.service;

import com.br.productservice.model.Product;
import com.br.productservice.model.ProductAttribute;
import com.br.productservice.repository.ProductAttributeRepository;
import com.br.productservice.service.dto.CreateAttributeDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductAttributeService {

    private final ProductAttributeRepository productAttributeRepository;

    public ProductAttributeService(ProductAttributeRepository productAttributeRepository) {
        this.productAttributeRepository = productAttributeRepository;
    }

    public List<ProductAttribute> saveAllProductAttributes(List<CreateAttributeDTO> attributeDTOS, Product product) {
        List<ProductAttribute> attributesToSave = new ArrayList<>();

        for (var attributeDTO : attributeDTOS) {
            ProductAttribute attributeToAdd = new ProductAttribute();
            attributeToAdd.setName(attributeDTO.getName());
            attributeToAdd.setValue(attributeDTO.getValue());
            attributeToAdd.setProduct(product);
            attributesToSave.add(attributeToAdd);
        }

        return productAttributeRepository.saveAll(attributesToSave);
    }
}

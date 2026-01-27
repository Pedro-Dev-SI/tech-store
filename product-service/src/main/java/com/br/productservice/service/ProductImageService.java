package com.br.productservice.service;

import com.br.productservice.model.Product;
import com.br.productservice.model.ProductImage;
import com.br.productservice.repository.ProductImageRepository;
import com.br.productservice.service.dto.CreateImageDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProductImageService {

    private final ProductImageRepository productImageRepository;

    public ProductImageService(ProductImageRepository productImageRepository) {
        this.productImageRepository = productImageRepository;
    }

    public List<ProductImage> saveAllProductImages(List<CreateImageDTO> imageDTOS, Product product) {

        List<ProductImage> imagesToSave = new ArrayList<>();
        int count = 0;

        for (var imageDTO : imageDTOS) {
            ProductImage imageToAdd = new ProductImage();
            imageToAdd.setUrl(imageDTO.getUrl());
            imageToAdd.setIsMain(count == 0);
            imageToAdd.setPosition(count);
            imageToAdd.setProduct(product);
            imageToAdd.setAltText(imageDTO.getAltText());
            imagesToSave.add(imageToAdd);
            count++;
        }

        return productImageRepository.saveAll(imagesToSave);
    }
}

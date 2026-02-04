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

    /**
     * Saves all product images with sequential positions and a single main image.
     *
     * @param imageDTOS List of image DTOs
     * @param product Product owner
     * @return List of saved images
     */
    public List<ProductImage> saveAllProductImages(List<CreateImageDTO> imageDTOS, Product product) {

        List<ProductImage> imagesToSave = new ArrayList<>();
        int position = 0;

        for (var imageDTO : imageDTOS) {
            ProductImage imageToAdd = new ProductImage();
            imageToAdd.setUrl(imageDTO.getUrl());
            imageToAdd.setIsMain(position == 0);
            imageToAdd.setPosition(position);
            imageToAdd.setProduct(product);
            imageToAdd.setAltText(imageDTO.getAltText());
            imagesToSave.add(imageToAdd);
            position++;
        }

        return productImageRepository.saveAll(imagesToSave);
    }

    /**
     * Replaces all images for a product.
     *
     * @param imageDTOS New list of images
     * @param product Product owner
     * @return List of saved images
     */
    public List<ProductImage> replaceAllProductImages(List<CreateImageDTO> imageDTOS, Product product) {
        productImageRepository.deleteByProductId(product.getId());
        if (imageDTOS == null || imageDTOS.isEmpty()) {
            return List.of();
        }
        return saveAllProductImages(imageDTOS, product);
    }

    /**
     * Delete ProductImage by id
     * @param imageId
     */
    public void delete(UUID imageId) {
        productImageRepository.deleteById(imageId);
    }
}

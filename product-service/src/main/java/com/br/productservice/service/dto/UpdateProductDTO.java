package com.br.productservice.service.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class UpdateProductDTO {
    private String sku;
    private String name;
    private String description;
    private String brand;
    private UUID categoryId;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private List<CreateImageDTO> productImages;
    private List<CreateAttributeDTO> productAttributes;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCompareAtPrice() {
        return compareAtPrice;
    }

    public void setCompareAtPrice(BigDecimal compareAtPrice) {
        this.compareAtPrice = compareAtPrice;
    }

    public List<CreateImageDTO> getProductImages() {
        return productImages;
    }

    public void setProductImages(List<CreateImageDTO> productImages) {
        this.productImages = productImages;
    }

    public List<CreateAttributeDTO> getProductAttributes() {
        return productAttributes;
    }

    public void setProductAttributes(List<CreateAttributeDTO> productAttributes) {
        this.productAttributes = productAttributes;
    }
}

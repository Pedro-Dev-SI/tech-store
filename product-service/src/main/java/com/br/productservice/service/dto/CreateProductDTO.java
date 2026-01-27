package com.br.productservice.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreateProductDTO {

    @NotNull
    @NotBlank
    private String sku;
    @NotNull
    @NotBlank
    private String name;
    private String description;
    @NotNull
    @NotBlank
    private String brand;
    @NotNull
    private UUID categoryId;
    @NotNull
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private List<CreateImageDTO> productImages;
    private List<CreateAttributeDTO> productAttributes;
}

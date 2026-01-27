package com.br.productservice.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    private String sku;

    @NotBlank
    @Size(min = 3, max = 200)
    private String name;

    private String description;

    @NotBlank
    private String brand;

    @NotNull
    private UUID categoryId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    @DecimalMin(value = "0.01")
    private BigDecimal compareAtPrice;

    @Valid
    private List<CreateImageDTO> productImages;

    @Valid
    private List<CreateAttributeDTO> productAttributes;
}

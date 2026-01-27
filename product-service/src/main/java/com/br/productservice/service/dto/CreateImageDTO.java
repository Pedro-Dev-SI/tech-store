package com.br.productservice.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreateImageDTO {

    @NotBlank
    private String url;

    private String altText;

    private Integer position;

    private Boolean isMain;
}

package com.br.productservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para filtros de busca de produtos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterDTO {
    
    private UUID categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String brand;
    private String search;
    private Boolean active;  // null = default (true para público, todos para admin)
    private String sortBy;   // "price", "name", "createdAt", "relevance"
    private String sortDirection; // "asc" ou "desc"
    private Integer page;     // default 0
    private Integer size;     // default 20, max 100
}


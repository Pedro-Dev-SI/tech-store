package com.br.productservice.service;

import com.br.productservice.exception.ResourceNotFoundException;
import com.br.productservice.model.Product;
import com.br.productservice.repository.ProductRepository;
import com.br.productservice.service.dto.ProductFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    /**
     * Lists paginated products with applied filters
     * 
     * @param filter Search filters
     * @param isAdmin If true, can see inactive products
     * @return Page of products
     */
    @Transactional(readOnly = true)
    public Page<Product> listProducts(ProductFilterDTO filter, boolean isAdmin) {
        // Configure pagination
        Pageable pageable = buildPageable(filter);
        
        // Define if shows only active products (default: true for public)
        boolean showOnlyActive = filter.getActive() != null ? filter.getActive() : !isAdmin;
        
        // Apply filters
        Page<Product> products;
        
        // Filter by category (includes subcategories)
        if (filter.getCategoryId() != null) {
            Set<UUID> categoryIds = categoryService.getCategoryIdsIncludingChildren(filter.getCategoryId());
            products = applyFiltersWithCategory(categoryIds, filter, showOnlyActive, pageable);
        } else {
            products = applyFiltersWithoutCategory(filter, showOnlyActive, pageable);
        }
        
        return products;
    }

    /**
     * Finds a product by slug
     * 
     * @param slug Product slug (e.g., "iphone-15-pro-max-256gb")
     * @return Found product
     * @throws IllegalArgumentException if slug is null or blank
     * @throws ResourceNotFoundException if product is not found
     */
    @Transactional(readOnly = true)
    public Product findBySlug(String slug) {
        // Validation: check null BEFORE calling methods
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Slug cannot be null or blank");
        }
        
        // Search in database and throw custom exception if not found
        return productRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Product", slug));
    }

    /**
     * Finds a product by id
     *
     * @param id Product id
     * @return Found product
     * @throws IllegalArgumentException if id is null
     * @throws ResourceNotFoundException if product is not found
     */
    public Product findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
    }

    /**
     * Applies filters when there is a category filter
     */
    private Page<Product> applyFiltersWithCategory(
            Set<UUID> categoryIds, 
            ProductFilterDTO filter, 
            boolean showOnlyActive, 
            Pageable pageable) {
        
        List<UUID> categoryList = new ArrayList<>(categoryIds);
        
        // Combine filters
        if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
            // Text search + category
            if (showOnlyActive) {
                return productRepository.findByCategoryIdInAndActiveTrueAndNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    categoryList, filter.getSearch(), pageable);
            } else {
                return productRepository.findByCategoryIdInAndNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    categoryList, filter.getSearch(), pageable);
            }
        } else if (filter.getBrand() != null && !filter.getBrand().trim().isEmpty()) {
            // Brand + category
            if (showOnlyActive) {
                return productRepository.findByCategoryIdInAndBrandIgnoreCaseAndActiveTrue(
                    categoryList, filter.getBrand(), pageable);
            } else {
                return productRepository.findByCategoryIdInAndBrandIgnoreCase(
                    categoryList, filter.getBrand(), pageable);
            }
        } else if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {
            // Price + category
            BigDecimal minPrice = filter.getMinPrice() != null ? filter.getMinPrice() : BigDecimal.ZERO;
            BigDecimal maxPrice = filter.getMaxPrice() != null ? filter.getMaxPrice() : new BigDecimal("999999.99");
            
            if (showOnlyActive) {
                return productRepository.findByCategoryIdInAndPriceBetweenAndActiveTrue(
                    categoryList, minPrice, maxPrice, pageable);
            } else {
                return productRepository.findByCategoryIdInAndPriceBetween(
                    categoryList, minPrice, maxPrice, pageable);
            }
        } else {
            // Category only
            if (showOnlyActive) {
                return productRepository.findByCategoryIdInAndActiveTrue(categoryList, pageable);
            } else {
                return productRepository.findByCategoryIdIn(categoryList, pageable);
            }
        }
    }

    /**
     * Applies filters when there is NO category filter
     */
    private Page<Product> applyFiltersWithoutCategory(
            ProductFilterDTO filter, 
            boolean showOnlyActive, 
            Pageable pageable) {
        
        if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
            // Text search
            if (showOnlyActive) {
                return productRepository.searchActiveByNameOrDescription(filter.getSearch(), pageable);
            } else {
                return productRepository.searchByNameOrDescription(filter.getSearch(), pageable);
            }
        } else if (filter.getBrand() != null && !filter.getBrand().trim().isEmpty()) {
            // Brand
            if (showOnlyActive) {
                return productRepository.findByBrandIgnoreCaseAndActiveTrue(filter.getBrand(), pageable);
            } else {
                return productRepository.findByBrandIgnoreCase(filter.getBrand(), pageable);
            }
        } else if (filter.getMinPrice() != null || filter.getMaxPrice() != null) {
            // Price
            BigDecimal minPrice = filter.getMinPrice() != null ? filter.getMinPrice() : BigDecimal.ZERO;
            BigDecimal maxPrice = filter.getMaxPrice() != null ? filter.getMaxPrice() : new BigDecimal("999999.99");
            
            if (showOnlyActive) {
                return productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice, pageable);
            } else {
                return productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
            }
        } else {
            // No specific filters
            if (showOnlyActive) {
                return productRepository.findByActiveTrue(pageable);
            } else {
                return productRepository.findAll(pageable);
            }
        }
    }

    /**
     * Builds the Pageable object with pagination and sorting
     */
    private Pageable buildPageable(ProductFilterDTO filter) {
        // Pagination
        int page = filter.getPage() != null && filter.getPage() >= 0 ? filter.getPage() : 0;
        int size = filter.getSize() != null ? Math.min(filter.getSize(), 100) : 20; // max 100
        
        // Sorting
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDirection()) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        // Map sorting fields
        String sortField = mapSortField(sortBy);
        
        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }

    /**
     * Maps the sorting field from DTO to entity field
     */
    private String mapSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "price" -> "price";
            case "name" -> "name";
            case "createdat", "created_at" -> "createdAt";
            case "relevance" -> "createdAt"; // Relevance uses createdAt as fallback
            default -> "createdAt";
        };
    }
}

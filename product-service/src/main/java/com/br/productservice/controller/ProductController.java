package com.br.productservice.controller;

import com.br.productservice.enums.RoleEnum;
import com.br.productservice.service.ProductService;
import com.br.productservice.service.dto.CreateAttributeDTO;
import com.br.productservice.service.dto.CreateImageDTO;
import com.br.productservice.service.dto.CreateProductDTO;
import com.br.productservice.service.dto.ProductAttributesResponse;
import com.br.productservice.service.dto.ProductFilterDTO;
import com.br.productservice.service.dto.ProductImageResponse;
import com.br.productservice.service.dto.ProductResponse;
import com.br.productservice.service.dto.UpdateProductDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Lists products with pagination and filters
     * 
     * @param categoryId Filter by category (includes subcategories)
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     * @param brand Filter by brand
     * @param search Text search in name and description
     * @param active Filter by active status (default: true for public)
     * @param sortBy Sort field: "price", "name", "createdAt", "relevance" (default: "createdAt")
     * @param sortDirection Sort direction: "asc" or "desc" (default: "desc")
     * @param page Page number (default: 0)
     * @param size Page size (default: 20, max: 100)
     * @return Page of products
     */
    @GetMapping()
    public ResponseEntity<Page<ProductResponse>> listProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        // Build filter DTO from request parameters
        ProductFilterDTO filter = ProductFilterDTO.builder()
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .brand(brand)
                .search(search)
                .active(active)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();
        
        log.info("REST - Request to list products with filters: categoryId={}, minPrice={}, maxPrice={}, brand={}, search={}, page={}, size={}", 
                categoryId, minPrice, maxPrice, brand, search, page, size);
        
        // TODO: Get isAdmin from authentication header (X-User-Role)
        // For now, defaulting to true (admin) - will be fixed when auth-service is ready
        boolean isAdmin = true;
        
        return ResponseEntity.ok().body(productService.listProducts(filter, isAdmin));
    }

    /**
     * Returns a product by id.
     *
     * @param id Product id
     * @return Product response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable UUID id) {
        log.info("REST - Request to get a product by id: {}", id);
        return ResponseEntity.ok().body(productService.findById(id));
    }

    @PostMapping("/list/all")
    public ResponseEntity<List<ProductResponse>> getAllByListIds(@RequestBody List<UUID> ids) {
        log.info("REST - Request to get all products by ids: {}", ids);
        return ResponseEntity.status(HttpStatus.OK).body(productService.findAllByIds(ids));
    }

    /**
     * Returns a product by slug.
     *
     * @param slug Product slug
     * @return Product response
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> findBySlug(@PathVariable String slug) {
        log.info("REST - Request to get a product by slug: {}", slug);
        return ResponseEntity.ok().body(productService.findBySlug(slug));
    }

    /**
     * Searches products by text query (name and description)
     * 
     * @param q Search query
     * @param page Page number (default: 0)
     * @param size Page size (default: 20, max: 100)
     * @param sortBy Sort field (default: "createdAt")
     * @param sortDirection Sort direction: "asc" or "desc" (default: "desc")
     * @return Page of products matching the search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection) {
        
        log.info("REST - Request to search products with query: '{}', page: {}, size: {}", q, page, size);
        
        // Validate query parameter
        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Build pagination
        int validSize = Math.min(size, 100); // Max 100
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, validSize, Sort.by(direction, sortBy));
        
        // Search products
        Page<ProductResponse> products = productService.searchProduct(q.trim(), pageable);
        
        return ResponseEntity.ok().body(products);
    }

    /**
     * Creates a new product.
     *
     * @param role User role - required ADMIN
     * @param productDTO Product data
     * @return Created product
     */
    @PostMapping()
    public ResponseEntity<?> create(@RequestHeader("X-User-Role") String role, @Valid @RequestBody CreateProductDTO productDTO){
        log.info("REST - Request to create a new product with the name: {}. SKU: {}", productDTO.getName(), productDTO.getSku());
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createNewProduct(productDTO));
    }

    /**
     * Partially updates an existing product (PATCH semantics).
     *
     * @param id Product id
     * @param productDTO Updated product data (only changed fields)
     * @return Updated product
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@RequestHeader("X-User-Role") String role, @PathVariable UUID id, @RequestBody UpdateProductDTO productDTO) {
        log.info("REST - Request to update the product with id: {}", id);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.status(HttpStatus.OK).body(productService.updateProduct(id, productDTO));
    }

    /**
     * Inactivate product by its id
     *
     * @param role
     * @param id   Product id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> inactivate(@RequestHeader("X-User-Role") String role, @PathVariable UUID id) {
        log.info("REST - Request to inactivate the product with id: {}", id);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        productService.inactivateProduct(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     *
     * @param id Product id for update
     * @param imageDTOS list of images to be set at the Product
     * @return list of ProductImageResponse
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<?> addNewImages(@RequestHeader("X-User-Role") String role, @PathVariable UUID id, @RequestBody List<CreateImageDTO> imageDTOS) {
        log.info("REST - Request to add new images to the Product with id: {}", id);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addNewImages(id, imageDTOS));
    }

    /**
     * Delete Product Images
     * @param id Product Image to be deleted
     * @return void
     */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<?> removeProductImage(@RequestHeader("X-User-Role") String role, @PathVariable UUID id, @PathVariable UUID imageId) {
        log.info("REST - Request to remove ProductImage: {}", imageId);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        productService.deleteProductImage(id, imageId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    /**
     *
     * @param id Product id for update
     * @param attributeDTOS list of attributes to be set at the Product
     * @return list of ProductAttributeResponse
     */
    @PostMapping("/{id}/attributes")
    public ResponseEntity<?> addNewAttributes(@RequestHeader("X-User-Role") String role, @PathVariable UUID id, @RequestBody List<CreateAttributeDTO> attributeDTOS) {
        log.info("REST - Request to add new attributes to the Product with id: {}", id);
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.addNewAttributes(id, attributeDTOS));
    }
}

package com.br.productservice.service;

import com.br.productservice.exception.BusinessException;
import com.br.productservice.exception.ResourceDuplicatedException;
import com.br.productservice.exception.ResourceNotFoundException;
import com.br.productservice.model.Product;
import com.br.productservice.repository.CategoryRepository;
import com.br.productservice.repository.ProductRepository;
import com.br.productservice.service.dto.CreateAttributeDTO;
import com.br.productservice.service.dto.CreateImageDTO;
import com.br.productservice.service.dto.CreateProductDTO;
import com.br.productservice.service.dto.ProductAttributesResponse;
import com.br.productservice.service.dto.ProductFilterDTO;
import com.br.productservice.service.dto.ProductImageResponse;
import com.br.productservice.service.dto.ProductResponse;
import com.br.productservice.service.dto.UpdateProductDTO;
import com.br.productservice.service.mapper.ProductAttributesMapper;
import com.br.productservice.service.mapper.ProductMapper;
import com.br.productservice.service.mapper.ProductImageMapper;
import com.br.productservice.utils.SlugUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final ProductImageService productImageService;
    private final ProductAttributeService productAttributeService;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final ProductAttributesMapper productAttributesMapper;

    public ProductService(ProductRepository productRepository, CategoryService categoryService, CategoryRepository categoryRepository, ProductImageService productImageService, ProductAttributeService productAttributeService, ProductMapper productMapper, ProductImageMapper productImageMapper, ProductAttributesMapper productAttributesMapper) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.categoryRepository = categoryRepository;
        this.productImageService = productImageService;
        this.productAttributeService = productAttributeService;
        this.productMapper = productMapper;
        this.productImageMapper = productImageMapper;
        this.productAttributesMapper = productAttributesMapper;
    }

    /**
     * Lists paginated products with applied filters
     * 
     * @param filter Search filters
     * @param isAdmin If true, can see inactive products
     * @return Page of products
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(ProductFilterDTO filter, boolean isAdmin) {
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
        
        return products.map(productMapper::toResponse);
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
    public ProductResponse findBySlug(String slug) {
        // Validation: check null BEFORE calling methods
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("Slug cannot be null or blank");
        }
        
        // Search in database and throw custom exception if not found
        Product product = productRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Product", slug));
        return productMapper.toResponse(product);
    }

    /**
     * Searches products by text (name and description)
     * Returns only active products (public endpoint)
     *
     * @param query Search query
     * @param pageable Pagination parameters
     * @return Page of active products matching the search
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProduct(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query param must not be null or blank");
        }

        // Search only in active products (public endpoint)
        return productRepository.searchActiveByNameOrDescription(query.trim(), pageable)
            .map(productMapper::toResponse);
    }

    /**
     * Finds a product by id
     *
     * @param id Product id
     * @return Found product
     * @throws IllegalArgumentException if id is null
     * @throws ResourceNotFoundException if product is not found
     */
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));
        return productMapper.toResponse(product);
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
            case "relevance" -> "createdAt";
            default -> "createdAt";
        };
    }


    /**
     * Create new Product and call external functions to save ProductImage and ProductAttrinute
     */
    @Transactional
    public ProductResponse createNewProduct(CreateProductDTO productDTO) {

        if (productRepository.existsBySku(productDTO.getSku())) {
            throw new ResourceDuplicatedException("Product", productDTO.getSku());
        }

        var existingActiveCategory = categoryRepository.findByIdAndActiveTrue(productDTO.getCategoryId());

        if (existingActiveCategory.isEmpty()) {
            throw new ResourceNotFoundException("Category", productDTO.getCategoryId().toString());
        }

        if (productDTO.getCompareAtPrice() != null && productDTO.getCompareAtPrice().compareTo(productDTO.getPrice()) <= 0) {
            throw new IllegalArgumentException("Compare at price must be greater than the actual product price");
        }

        if (productDTO.getProductImages() != null && productDTO.getProductImages().size() > 10) {
            throw new IllegalArgumentException("Maximum of 10 images per product");
        }

        if (productDTO.getName() == null || productDTO.getName().length() < 3 || productDTO.getName().length() > 200) {
            throw new IllegalArgumentException("Product name must be between 3 and 200 characters");
        }

        // Generate base slug
        String baseSlug = SlugUtils.generateSlug(productDTO.getName());
        String pattern = baseSlug + "-%";
        
        // Find all slugs that match base slug or have suffix
        List<String> slugs = productRepository.findSlugsWithSuffix(baseSlug, pattern);
        
        int maxSuffix = 0;
        
        // Check if base slug exists (without suffix)
        if (slugs.contains(baseSlug)) {
            maxSuffix = 1;
        }
        
        // Process slugs with numeric suffix only
        for (String slug : slugs) {
            if (slug.startsWith(baseSlug + "-")) {
                String suffixPart = slug.substring(baseSlug.length() + 1);
                try {
                    // Only count numeric suffixes (ignore non-numeric like "256gb")
                    int suffix = Integer.parseInt(suffixPart);
                    maxSuffix = Math.max(maxSuffix, suffix);
                } catch (NumberFormatException e) {
                    // Ignore slugs with non-numeric suffix (e.g., "iphone-15-pro-max-256gb")
                }
            }
        }
        
        // Create product entity
        Product product = new Product();
        product.setSku(productDTO.getSku());
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setBrand(productDTO.getBrand());
        product.setCategory(existingActiveCategory.get());
        product.setPrice(productDTO.getPrice());
        product.setCompareAtPrice(productDTO.getCompareAtPrice());
        product.setActive(true);

        Product savedProduct = null;
        int currentSuffix = maxSuffix;

        for (int attempt = 0; attempt < 3; attempt++) {
            String candidateSlug = currentSuffix > 0 ? baseSlug + "-" + (currentSuffix + 1) : baseSlug;
            product.setSlug(candidateSlug);
            try {
                savedProduct = productRepository.save(product);
                break;
            } catch (DataIntegrityViolationException e) {
                if (productRepository.existsBySku(productDTO.getSku())) {
                    throw new ResourceDuplicatedException("Product", productDTO.getSku());
                }
                currentSuffix++;
                if (attempt == 2) {
                    throw new ResourceDuplicatedException("Product slug", candidateSlug);
                }
            }
        }

        if (savedProduct == null) {
            throw new IllegalStateException("Could not persist product");
        }


        if (productDTO.getProductImages() != null && !productDTO.getProductImages().isEmpty()) {
            productImageService.saveAllProductImages(productDTO.getProductImages(), savedProduct);
        }

        if (productDTO.getProductAttributes() != null && !productDTO.getProductAttributes().isEmpty()) {
            productAttributeService.saveAllProductAttributes(productDTO.getProductAttributes(), savedProduct);
        }

        //TODO - Update the Inventory Service

        return productMapper.toResponse(savedProduct);
    }

    /**
     * Partially updates a product and applies validations only for provided fields.
     *
     * @param id Product id
     * @param productDTO Updated product data (only changed fields)
     * @return Updated product response
     */
    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductDTO productDTO) {

        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        if (productDTO == null) {
            throw new IllegalArgumentException("Product data cannot be null");
        }

        Product productForUpdate = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));

        if (productDTO.getSku() != null) {
            if (productDTO.getSku().isBlank()) {
                throw new IllegalArgumentException("SKU cannot be blank");
            }
            if (!productDTO.getSku().equals(productForUpdate.getSku())
                && productRepository.existsBySku(productDTO.getSku())) {
                throw new ResourceDuplicatedException("Product", productDTO.getSku());
            }
            productForUpdate.setSku(productDTO.getSku());
        }

        if (productDTO.getPrice() != null && productDTO.getPrice().compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("Product price is lower than 0.01");
        }

        if (productDTO.getProductImages() != null && productDTO.getProductImages().size() > 10) {
            throw new IllegalArgumentException("Maximum of 10 images per product");
        }

        if (productDTO.getCompareAtPrice() != null) {
            BigDecimal priceForCheck = productDTO.getPrice() != null ? productDTO.getPrice() : productForUpdate.getPrice();
            if (priceForCheck == null) {
                throw new IllegalArgumentException("Price must be defined before compareAtPrice");
            }
            if (productDTO.getCompareAtPrice().compareTo(priceForCheck) <= 0) {
                throw new IllegalArgumentException("Compare at price must be greater than the actual product price");
            }
        }

        if (productDTO.getName() != null) {
            if (productDTO.getName().length() < 3 || productDTO.getName().length() > 200) {
                throw new IllegalArgumentException("Product name must be between 3 and 200 characters");
            }
        }

        if (productDTO.getCategoryId() != null
            && (productForUpdate.getCategory() == null
                || !productDTO.getCategoryId().equals(productForUpdate.getCategory().getId()))) {
            var existingActiveCategory = categoryRepository.findByIdAndActiveTrue(productDTO.getCategoryId());
            if (existingActiveCategory.isEmpty()) {
                throw new ResourceNotFoundException("Category", productDTO.getCategoryId().toString());
            }
            productForUpdate.setCategory(existingActiveCategory.get());
        }

        String currentName = productForUpdate.getName();
        boolean nameChanged = productDTO.getName() != null && !productDTO.getName().equals(currentName);

        if (productDTO.getName() != null) {
            productForUpdate.setName(productDTO.getName());
        }

        if (productDTO.getDescription() != null) {
            productForUpdate.setDescription(productDTO.getDescription());
        }

        if (productDTO.getBrand() != null) {
            productForUpdate.setBrand(productDTO.getBrand());
        }

        if (productDTO.getPrice() != null) {
            productForUpdate.setPrice(productDTO.getPrice());
        }

        if (productDTO.getCompareAtPrice() != null) {
            productForUpdate.setCompareAtPrice(productDTO.getCompareAtPrice());
        }

        if (nameChanged) {
            String baseSlug = SlugUtils.generateSlug(productDTO.getName());
            String pattern = baseSlug + "-%";

            List<String> slugs = productRepository.findSlugsWithSuffix(baseSlug, pattern);
            slugs.remove(productForUpdate.getSlug());

            int maxSuffix = 0;
            if (slugs.contains(baseSlug)) {
                maxSuffix = 1;
            }

            for (String slug : slugs) {
                if (slug.startsWith(baseSlug + "-")) {
                    String suffixPart = slug.substring(baseSlug.length() + 1);
                    try {
                        int suffix = Integer.parseInt(suffixPart);
                        maxSuffix = Math.max(maxSuffix, suffix);
                    } catch (NumberFormatException e) {
                        // Ignore non-numeric suffixes
                    }
                }
            }

            int currentSuffix = maxSuffix;
            for (int attempt = 0; attempt < 3; attempt++) {
                String candidateSlug = currentSuffix > 0 ? baseSlug + "-" + (currentSuffix + 1) : baseSlug;
                productForUpdate.setSlug(candidateSlug);
                try {
                    Product savedProduct = productRepository.save(productForUpdate);
                    if (productDTO.getProductImages() != null) {
                        productImageService.replaceAllProductImages(productDTO.getProductImages(), savedProduct);
                    }
                    if (productDTO.getProductAttributes() != null) {
                        productAttributeService.replaceAllProductAttributes(productDTO.getProductAttributes(), savedProduct);
                    }
                    return productMapper.toResponse(savedProduct);
                } catch (DataIntegrityViolationException e) {
                    if (productDTO.getSku() != null && productRepository.existsBySku(productDTO.getSku())) {
                        throw new ResourceDuplicatedException("Product", productDTO.getSku());
                    }
                    currentSuffix++;
                    if (attempt == 2) {
                        throw new ResourceDuplicatedException("Product slug", candidateSlug);
                    }
                }
            }

            throw new IllegalStateException("Could not persist product");
        }

        Product savedProduct = productRepository.save(productForUpdate);
        if (productDTO.getProductImages() != null) {
            productImageService.replaceAllProductImages(productDTO.getProductImages(), savedProduct);
        }
        if (productDTO.getProductAttributes() != null) {
            productAttributeService.replaceAllProductAttributes(productDTO.getProductAttributes(), savedProduct);
        }
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Inactivate product by its Id
     * @param id
     */
    @Transactional
    public void inactivateProduct(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException("Id can not be null");
        }

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));

        product.setActive(false);
        productRepository.save(product);
    }

    /**
     *
     * @param id Product id
     * @param imageDTOS images to be set in the Product
     * @return list of ProductImageResponse
     */
    @Transactional
    public List<ProductImageResponse> addNewImages(UUID id, List<CreateImageDTO> imageDTOS) {

        if (id == null) {
            throw new IllegalArgumentException("Product id must not be null");
        }

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));

        if (product.getProductImages() != null && product.getProductImages().size() > 10) {
            throw new BusinessException("Cannot insert more than 10 images in one Product");
        }

        return productImageService.saveAllProductImages(imageDTOS, product)
            .stream()
            .map(productImageMapper::toResponse)
            .toList();
    }

    /**
     *
     * @param id Product id
     * @param attributeDTOS list of attributes
     * @return list of ProductAttributeResponse
     */
    @Transactional
    public  List<ProductAttributesResponse> addNewAttributes(UUID id, List<CreateAttributeDTO> attributeDTOS) {
        if (id == null) {
            throw new IllegalArgumentException("Product id must not be null");
        }

        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));

        return productAttributeService.saveAllProductAttributes(attributeDTOS, product)
                .stream()
                .map(productAttributesMapper::toResponse)
                .toList();
    }

    /**
     * Call the delete method on ProductImageService to delete the image.
     * @param id Product id
     * @param imageId image id
     */
    @Transactional
    public void deleteProductImage(UUID id, UUID imageId) {

        if (id == null || imageId == null) {
            throw new IllegalArgumentException("Product id and Image id must be informed");
        }

        if (productRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Product", id.toString());
        }

        productImageService.delete(imageId);
    }
}

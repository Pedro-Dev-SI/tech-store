package com.br.productservice.service;

import com.br.productservice.exception.ResourceDuplicatedException;
import com.br.productservice.model.Category;
import com.br.productservice.model.Product;
import com.br.productservice.repository.CategoryRepository;
import com.br.productservice.repository.ProductRepository;
import com.br.productservice.service.dto.CreateProductDTO;
import com.br.productservice.service.dto.ProductResponse;
import com.br.productservice.service.dto.UpdateProductDTO;
import com.br.productservice.service.mapper.ProductAttributesMapper;
import com.br.productservice.service.mapper.ProductImageMapper;
import com.br.productservice.service.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageService productImageService;

    @Mock
    private ProductAttributeService productAttributeService;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private ProductAttributesMapper productAttributesMapper;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private ProductResponse response;

    @BeforeEach
    void setUp() {
        response = new ProductResponse(
            UUID.randomUUID(),
            "SKU",
            "Produto",
            "produto",
            "Desc",
            "Brand",
            UUID.randomUUID(),
            BigDecimal.TEN,
            BigDecimal.valueOf(20),
            true,
            null,
            null
        );
    }

    @Test
    void createNewProduct_generatesSlug() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);

        CreateProductDTO dto = new CreateProductDTO(
            "SKU-1",
            "Iphone 15 Pro",
            "Desc",
            "Apple",
            categoryId,
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(200),
            null,
            null
        );

        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.findByIdAndActiveTrue(categoryId)).thenReturn(Optional.of(category));
        when(productRepository.findSlugsWithSuffix(anyString(), anyString())).thenReturn(List.of());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any(Product.class))).thenReturn(response);

        productService.createNewProduct(dto);

        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertEquals("iphone-15-pro", saved.getSlug());
        assertEquals("SKU-1", saved.getSku());
    }

    @Test
    void createNewProduct_duplicateSku_throws() {
        CreateProductDTO dto = new CreateProductDTO(
            "SKU-1",
            "Iphone 15 Pro",
            "Desc",
            "Apple",
            UUID.randomUUID(),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(200),
            null,
            null
        );

        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThrows(ResourceDuplicatedException.class, () -> productService.createNewProduct(dto));
    }

    @Test
    void createNewProduct_compareAtPriceInvalid_throws() {
        UUID categoryId = UUID.randomUUID();
        CreateProductDTO dto = new CreateProductDTO(
            "SKU-1",
            "Iphone 15 Pro",
            "Desc",
            "Apple",
            categoryId,
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(90),
            null,
            null
        );

        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.findByIdAndActiveTrue(categoryId)).thenReturn(Optional.of(new Category()));

        assertThrows(IllegalArgumentException.class, () -> productService.createNewProduct(dto));
    }

    @Test
    void updateProduct_nameChanged_updatesSlugWithSuffix() {
        UUID productId = UUID.randomUUID();

        Product existing = new Product();
        existing.setId(productId);
        existing.setName("Iphone");
        existing.setSlug("iphone");

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.findSlugsWithSuffix(anyString(), anyString()))
            .thenReturn(new ArrayList<>(List.of("iphone-pro", "iphone-pro-2")));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productMapper.toResponse(any(Product.class))).thenReturn(response);

        UpdateProductDTO dto = new UpdateProductDTO();
        dto.setName("Iphone Pro");

        productService.updateProduct(productId, dto);

        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertEquals("iphone-pro-3", saved.getSlug());
        assertEquals("Iphone Pro", saved.getName());
    }

    @Test
    void updateProduct_duplicateSku_throws() {
        UUID productId = UUID.randomUUID();

        Product existing = new Product();
        existing.setId(productId);
        existing.setSku("SKU-OLD");

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySku("SKU-NEW")).thenReturn(true);

        UpdateProductDTO dto = new UpdateProductDTO();
        dto.setSku("SKU-NEW");

        assertThrows(ResourceDuplicatedException.class, () -> productService.updateProduct(productId, dto));
    }
}

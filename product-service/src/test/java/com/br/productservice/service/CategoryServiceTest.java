package com.br.productservice.service;

import com.br.productservice.exception.BusinessException;
import com.br.productservice.exception.ResourceDuplicatedException;
import com.br.productservice.model.Category;
import com.br.productservice.repository.CategoryRepository;
import com.br.productservice.service.dto.CategoryResponse;
import com.br.productservice.service.dto.CreateCategoryDTO;
import com.br.productservice.service.dto.UpdateCategoryDTO;
import com.br.productservice.service.mapper.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Captor
    private ArgumentCaptor<Category> categoryCaptor;

    private CategoryResponse response;

    @BeforeEach
    void setUp() {
        response = new CategoryResponse(UUID.randomUUID(), "Eletronicos", "eletronicos", "Desc", null, true);
    }

    @Test
    void createNewCategory_root_generatesSlug() {
        CreateCategoryDTO dto = new CreateCategoryDTO("Eletronicos", "Desc", null);

        when(categoryRepository.findByParentIsNullAndNameIgnoreCase("Eletronicos"))
            .thenReturn(Optional.empty());
        when(categoryRepository.findSlugsWithSuffix(anyString(), anyString()))
            .thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(response);

        categoryService.createNewCategory(dto);

        verify(categoryRepository).save(categoryCaptor.capture());
        Category saved = categoryCaptor.getValue();
        assertEquals("eletronicos", saved.getSlug());
        assertEquals("Eletronicos", saved.getName());
    }

    @Test
    void createNewCategory_duplicateName_sameLevel_throws() {
        CreateCategoryDTO dto = new CreateCategoryDTO("Eletronicos", "Desc", null);

        Category existing = new Category();
        existing.setId(UUID.randomUUID());

        when(categoryRepository.findByParentIsNullAndNameIgnoreCase("Eletronicos"))
            .thenReturn(Optional.of(existing));

        assertThrows(ResourceDuplicatedException.class, () -> categoryService.createNewCategory(dto));
    }

    @Test
    void createNewCategory_hierarchyExceeds_throws() {
        UUID parentId = UUID.randomUUID();

        Category root = new Category();
        Category level2 = new Category();
        Category level3 = new Category();
        level2.setParent(root);
        level3.setParent(level2);

        when(categoryRepository.findByIdAndActiveTrue(parentId))
            .thenReturn(Optional.of(level3));

        CreateCategoryDTO dto = new CreateCategoryDTO("Apple", "Desc", parentId);

        assertThrows(BusinessException.class, () -> categoryService.createNewCategory(dto));
    }

    @Test
    void updateCategory_name_updatesSlug() {
        UUID categoryId = UUID.randomUUID();

        Category existing = new Category();
        existing.setId(categoryId);
        existing.setName("Celulares");
        existing.setSlug("celulares");
        existing.setParent(null);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByParentIsNullAndNameIgnoreCase("Smartphones"))
            .thenReturn(Optional.empty());
        when(categoryRepository.findSlugsWithSuffix(anyString(), anyString()))
            .thenReturn(new ArrayList<>(List.of("smartphones", "smartphones-2")));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(response);

        UpdateCategoryDTO dto = new UpdateCategoryDTO();
        dto.setName("Smartphones");

        categoryService.updateCategory(categoryId, dto);

        verify(categoryRepository).save(categoryCaptor.capture());
        Category saved = categoryCaptor.getValue();
        assertEquals("smartphones-3", saved.getSlug());
        assertEquals("Smartphones", saved.getName());
    }

    @Test
    void updateCategory_duplicateName_sameLevel_throws() {
        UUID categoryId = UUID.randomUUID();

        Category existing = new Category();
        existing.setId(categoryId);
        existing.setName("Audio");
        existing.setSlug("audio");
        existing.setParent(null);

        Category other = new Category();
        other.setId(UUID.randomUUID());

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByParentIsNullAndNameIgnoreCase("Audio"))
            .thenReturn(Optional.of(other));

        UpdateCategoryDTO dto = new UpdateCategoryDTO();
        dto.setName("Audio");

        assertThrows(ResourceDuplicatedException.class, () -> categoryService.updateCategory(categoryId, dto));
    }
}

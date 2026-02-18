package com.br.productservice.service;

import com.br.productservice.exception.BusinessException;
import com.br.productservice.exception.ResourceDuplicatedException;
import com.br.productservice.exception.ResourceNotFoundException;
import com.br.productservice.model.Category;
import com.br.productservice.repository.CategoryRepository;
import com.br.productservice.repository.ProductRepository;
import com.br.productservice.service.dto.CategoryResponse;
import com.br.productservice.service.dto.CreateCategoryDTO;
import com.br.productservice.service.dto.UpdateCategoryDTO;
import com.br.productservice.service.mapper.CategoryMapper;
import com.br.productservice.utils.SlugUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.productRepository = productRepository;
    }

    /**
     * Busca todas as categorias incluindo subcategorias recursivamente
     */
    public Set<UUID> getCategoryIdsIncludingChildren(UUID categoryId) {
        Set<UUID> categoryIds = new HashSet<>();

        categoryIds.add(categoryId);

        //Busca recursiva de subcategiorias
        findChildrenRecursive(categoryId, categoryIds);

        return categoryIds;

    }

    /**
     * Busca subcategorias recursivamente (até 3 níveis conforme regra de negócio)
     */
    private void findChildrenRecursive(UUID parentId, Set<UUID> categoryIds) {
        List<Category> children = categoryRepository.findByParentId(parentId);

        for (Category child : children) {
            if (child.getActive()) {
                categoryIds.add(child.getId());
                findChildrenRecursive(child.getId(), categoryIds);
            }
        }
    }

    /**
     * It will return the list of all categories in the database
     * @param isAdmin check if its admin it will return actives and inactives
     * @return list of CategoryResponse
     */
    public List<CategoryResponse> listAll(boolean isAdmin) {

        if (isAdmin) {
            List<Category> categories = categoryRepository.findAll();

            return categories.stream().map(categoryMapper::toResponse).toList();
        }

        List<Category> categories = categoryRepository.findByActiveTrue();

        return categories.stream().map(categoryMapper::toResponse).toList();
    }

    /**
     * It will return a CategoryResponse by its id
     * @param id
     * @return CategoryResponse
     */
    public CategoryResponse findById(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException("Category id must not be null");
        }

        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));

        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse createNewCategory(CreateCategoryDTO categoryDTO) {

        if (categoryDTO == null) {
            throw new IllegalArgumentException("Category data must not be null");
        }

        if (categoryDTO.getName() == null || categoryDTO.getName().isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }

        Category parent = null;
        if (categoryDTO.getParentId() != null) {
            parent = categoryRepository.findByIdAndActiveTrue(categoryDTO.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Category parent", categoryDTO.getParentId().toString()));

            int depth = 1;
            Category current = parent;
            while (current.getParent() != null) {
                depth++;
                current = current.getParent();
            }

            if (depth >= 3) {
                throw new BusinessException("Não é permitido criar mais de 3 níveis na hierarquia");
            }
        }

        if (parent == null) {
            if (categoryRepository.findByParentIsNullAndNameIgnoreCase(categoryDTO.getName()).isPresent()) {
                throw new ResourceDuplicatedException("Category name", categoryDTO.getName());
            }
        } else {
            if (categoryRepository.findByParentIdAndNameIgnoreCase(parent.getId(), categoryDTO.getName()).isPresent()) {
                throw new ResourceDuplicatedException("Category name", categoryDTO.getName());
            }
        }

        // Generate base slug
        String baseSlug = SlugUtils.generateSlug(categoryDTO.getName());
        String pattern = baseSlug + "-%";

        List<String> slugs = categoryRepository.findSlugsWithSuffix(baseSlug, pattern);

        int maxSuffix = 0;

        // Check if the base slug exists (without suffix)
        if (slugs.contains(baseSlug)) {
            maxSuffix = 1;
        }

        // Process slugs with numeric suffix only
        for (String slug : slugs) {
            if (slug.startsWith(baseSlug + "-")) {
                String suffixPart = slug.substring(baseSlug.length() + 1);
                try {
                    int suffix = Integer.parseInt(suffixPart);
                    maxSuffix = Math.max(maxSuffix, suffix);
                } catch (NumberFormatException e) {
                    // Ignora sufixos não numéricos
                }
            }
        }

        // Create category entity
        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        category.setParent(parent);
        category.setActive(true);

        Category savedCategory = null;
        int currentSuffix = maxSuffix;

        for (int attempt = 0; attempt < 3; attempt++) {
            String candidateSlug = currentSuffix > 0 ? baseSlug + "-" + (currentSuffix + 1) : baseSlug;
            category.setSlug(candidateSlug);
            try {
                savedCategory = categoryRepository.save(category);
                break;
            } catch (DataIntegrityViolationException e) {
                if (categoryRepository.existsBySlug(candidateSlug)) {
                    currentSuffix++;
                    if (attempt == 2) {
                        throw new ResourceDuplicatedException("Category slug", candidateSlug);
                    }
                } else {
                    throw e;
                }
            }
        }

        if (savedCategory == null) {
            throw new IllegalStateException("Could not persist category");
        }

        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, UpdateCategoryDTO categoryDTO) {

        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        if (categoryDTO == null) {
            throw new IllegalArgumentException("Category data must not be null");
        }

        Category categoryForUpdate = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));

        boolean nameProvided = categoryDTO.getName() != null;
        boolean parentProvided = categoryDTO.getParentId() != null;

        if (nameProvided && categoryDTO.getName().isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }

        Category newParent = categoryForUpdate.getParent();
        if (parentProvided) {
            newParent = categoryRepository.findByIdAndActiveTrue(categoryDTO.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("Category parent", categoryDTO.getParentId().toString()));

            int depth = 1;
            Category current = newParent;
            while (current != null && current.getParent() != null) {
                depth++;
                current = current.getParent();
            }

            if (depth >= 3) {
                throw new BusinessException("Não é permitido criar mais de 3 níveis na hierarquia");
            }
        }

        if (nameProvided || parentProvided) {
            String nameToCheck = nameProvided ? categoryDTO.getName() : categoryForUpdate.getName();
            Category targetParent = parentProvided ? newParent : categoryForUpdate.getParent();
            if (targetParent == null) {
                Optional<Category> existing = categoryRepository.findByParentIsNullAndNameIgnoreCase(nameToCheck);
                if (existing.isPresent() && !existing.get().getId().equals(categoryForUpdate.getId())) {
                    throw new ResourceDuplicatedException("Category name", nameToCheck);
                }
            } else {
                Optional<Category> existing = categoryRepository.findByParentIdAndNameIgnoreCase(targetParent.getId(), nameToCheck);
                if (existing.isPresent() && !existing.get().getId().equals(categoryForUpdate.getId())) {
                    throw new ResourceDuplicatedException("Category name", nameToCheck);
                }
            }
        }

        if (nameProvided) {
            String baseSlug = SlugUtils.generateSlug(categoryDTO.getName());
            String pattern = baseSlug + "-%";

            List<String> slugs = categoryRepository.findSlugsWithSuffix(baseSlug, pattern);
            slugs.remove(categoryForUpdate.getSlug());

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
                        // Ignora sufixos não numéricos
                    }
                }
            }

            String candidateSlug = maxSuffix > 0 ? baseSlug + "-" + (maxSuffix + 1) : baseSlug;
            categoryForUpdate.setSlug(candidateSlug);

            categoryForUpdate.setName(categoryDTO.getName());
        }

        if (categoryDTO.getDescription() != null) {
            categoryForUpdate.setDescription(categoryDTO.getDescription());
        }

        if (parentProvided) {
            categoryForUpdate.setParent(newParent);
        }

        if (categoryDTO.getActive() != null) {
            categoryForUpdate.setActive(categoryDTO.getActive());
        }

        Category savedCategory = categoryRepository.save(categoryForUpdate);
        return categoryMapper.toResponse(savedCategory);
    }

    /**
     * Deactivates a category. If it has children, they are deactivated too.
     * If any product is linked to any category in this subtree, it throws BusinessException.
     */
    @Transactional
    public void deactivateCategory(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Category id must not be null");
        }

        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id.toString()));

        Set<UUID> idsToDeactivate = getCategoryIdsIncludingChildren(category.getId());

        if (productRepository.existsByCategoryIdIn(idsToDeactivate.stream().toList())) {
            throw new BusinessException("Cannot deactivate category with products linked");
        }

        List<Category> categories = categoryRepository.findAllById(idsToDeactivate);
        for (Category c : categories) {
            c.setActive(false);
        }

        categoryRepository.saveAll(categories);
    }
}

package com.br.productservice.service;

import com.br.productservice.model.Category;
import com.br.productservice.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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
}

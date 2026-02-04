package com.br.productservice.repository;

import com.br.productservice.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    
    // Buscar por slug (único)
    Optional<Category> findBySlug(String slug);
    
    // Listar categorias ativas
    List<Category> findByActiveTrue();

    Optional<Category> findByIdAndActiveTrue(UUID id);
    
    // Buscar categorias filhas de uma categoria pai
    List<Category> findByParentId(UUID parentId);

    Optional<Category> findByParentIdAndNameIgnoreCase(UUID parentId, String name);

    Optional<Category> findByParentIsNullAndNameIgnoreCase(String name);
    
    // Verificar se slug existe
    boolean existsBySlug(String slug);

    // Finds all slugs that match the base slug or have numeric suffix
    @Query("SELECT c.slug FROM Category c WHERE " +
            "c.slug = :baseSlug OR " +
            "c.slug LIKE :baseSlugPattern " +
            "ORDER BY c.slug DESC")
    List<String> findSlugsWithSuffix(
            @Param("baseSlug") String baseSlug,
            @Param("baseSlugPattern") String baseSlugPattern);
}

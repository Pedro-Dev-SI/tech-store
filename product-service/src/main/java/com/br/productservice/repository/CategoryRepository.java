package com.br.productservice.repository;

import com.br.productservice.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
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
    
    // Buscar categorias filhas de uma categoria pai
    List<Category> findByParentId(UUID parentId);
    
    // Verificar se slug existe
    boolean existsBySlug(String slug);
}


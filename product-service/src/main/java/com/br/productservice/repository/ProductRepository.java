package com.br.productservice.repository;

import com.br.productservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    
    // Buscar por SKU (único)
    Optional<Product> findBySku(String sku);
    
    // Buscar por slug (único)
    Optional<Product> findBySlug(String slug);
    
    // Verificar se SKU existe
    boolean existsBySku(String sku);
    
    // Verificar se slug existe
    boolean existsBySlug(String slug);
    
    // Buscar produtos ativos com paginação
    Page<Product> findByActiveTrue(Pageable pageable);
    
    // Buscar produtos por categoria (incluindo inativos para admin)
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);
    
    // Buscar produtos ativos por categoria
    Page<Product> findByCategoryIdAndActiveTrue(UUID categoryId, Pageable pageable);
    
    // Buscar por marca
    Page<Product> findByBrandIgnoreCase(String brand, Pageable pageable);
    
    // Buscar produtos ativos por marca
    Page<Product> findByBrandIgnoreCaseAndActiveTrue(String brand, Pageable pageable);
    
    // Busca textual no nome e descrição (case-insensitive)
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Product> searchByNameOrDescription(@Param("search") String search, Pageable pageable);
    
    // Busca textual apenas em produtos ativos
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchActiveByNameOrDescription(@Param("search") String search, Pageable pageable);
    
    // Buscar por faixa de preço
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Buscar produtos ativos por faixa de preço
    Page<Product> findByPriceBetweenAndActiveTrue(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Buscar produtos por lista de categorias (inclui subcategorias)
    Page<Product> findByCategoryIdIn(List<UUID> categoryIds, Pageable pageable);
    
    // Buscar produtos ativos por lista de categorias
    Page<Product> findByCategoryIdInAndActiveTrue(List<UUID> categoryIds, Pageable pageable);
    
    // Buscar por lista de categorias e marca
    Page<Product> findByCategoryIdInAndBrandIgnoreCase(List<UUID> categoryIds, String brand, Pageable pageable);
    
    // Buscar produtos ativos por lista de categorias e marca
    Page<Product> findByCategoryIdInAndBrandIgnoreCaseAndActiveTrue(List<UUID> categoryIds, String brand, Pageable pageable);
    
    // Buscar por lista de categorias e faixa de preço
    Page<Product> findByCategoryIdInAndPriceBetween(List<UUID> categoryIds, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Buscar produtos ativos por lista de categorias e faixa de preço
    Page<Product> findByCategoryIdInAndPriceBetweenAndActiveTrue(List<UUID> categoryIds, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    
    // Buscar por lista de categorias e busca textual
    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findByCategoryIdInAndNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        @Param("categoryIds") List<UUID> categoryIds, 
        @Param("search") String search, 
        Pageable pageable);
    
    // Buscar produtos ativos por lista de categorias e busca textual
    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds AND p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findByCategoryIdInAndActiveTrueAndNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        @Param("categoryIds") List<UUID> categoryIds, 
        @Param("search") String search, 
        Pageable pageable);

    // Finds all slugs that match the base slug or have numeric suffix
    @Query("SELECT p.slug FROM Product p WHERE " +
           "p.slug = :baseSlug OR " +
           "p.slug LIKE :baseSlugPattern " +
           "ORDER BY p.slug DESC")
    List<String> findSlugsWithSuffix(
        @Param("baseSlug") String baseSlug,
        @Param("baseSlugPattern") String baseSlugPattern);
}


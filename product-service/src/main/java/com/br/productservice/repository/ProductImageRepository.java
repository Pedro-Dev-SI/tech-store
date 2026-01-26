package com.br.productservice.repository;

import com.br.productservice.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    
    // Buscar todas as imagens de um produto ordenadas por position
    List<ProductImage> findByProductIdOrderByPositionAsc(UUID productId);
    
    // Buscar imagem principal de um produto
    Optional<ProductImage> findByProductIdAndIsMainTrue(UUID productId);
    
    // Contar quantas imagens um produto tem
    long countByProductId(UUID productId);
    
    // Verificar se produto tem imagem principal
    boolean existsByProductIdAndIsMainTrue(UUID productId);
}


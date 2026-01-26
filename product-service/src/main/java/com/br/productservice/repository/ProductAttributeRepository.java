package com.br.productservice.repository;

import com.br.productservice.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {
    
    // Buscar todos os atributos de um produto
    List<ProductAttribute> findByProductId(UUID productId);
    
    // Buscar atributos por nome (ex: todas as cores de um produto)
    List<ProductAttribute> findByProductIdAndName(UUID productId, String name);
}


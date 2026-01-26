-- =============================================
-- Migration V1: Schema inicial do Product Service
-- Cria as tabelas: category, product, product_image, product_attribute
-- =============================================

-- Habilita extensão para gerar UUIDs (se não existir)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================
-- Tabela: category (Categorias de produtos)
-- =============================================
CREATE TABLE category (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(100) NOT NULL,
  slug VARCHAR(120) NOT NULL UNIQUE,
  description TEXT,
  parent_id UUID REFERENCES category(id),  -- Hierarquia (auto-referência)
  active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para category
CREATE INDEX idx_category_slug ON category(slug);
CREATE INDEX idx_category_parent_id ON category(parent_id);
CREATE INDEX idx_category_active ON category(active);

-- =============================================
-- Tabela: product (Produtos)
-- =============================================
CREATE TABLE product (
 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
 sku VARCHAR(50) NOT NULL UNIQUE,
 name VARCHAR(200) NOT NULL,
 slug VARCHAR(220) NOT NULL UNIQUE,
 description TEXT,
 brand VARCHAR(100) NOT NULL,
 category_id UUID NOT NULL REFERENCES category(id),
 price DECIMAL(10, 2) NOT NULL CHECK (price >= 0.01),
 compare_at_price DECIMAL(10, 2),
 active BOOLEAN NOT NULL DEFAULT true,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Regra: compare_at_price deve ser maior que price (se informado)
     CONSTRAINT chk_compare_price CHECK (
         compare_at_price IS NULL OR compare_at_price > price
         )
);

-- Índices para product
CREATE INDEX idx_product_sku ON product(sku);
CREATE INDEX idx_product_slug ON product(slug);
CREATE INDEX idx_product_category_id ON product(category_id);
CREATE INDEX idx_product_brand ON product(brand);
CREATE INDEX idx_product_price ON product(price);
CREATE INDEX idx_product_active ON product(active);

-- =============================================
-- Tabela: product_image (Imagens dos produtos)
-- =============================================
CREATE TABLE product_image (
   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
   product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
   url VARCHAR(500) NOT NULL,
   alt_text VARCHAR(200),
   position INTEGER NOT NULL DEFAULT 0,
   is_main BOOLEAN NOT NULL DEFAULT false
);

-- Índice para product_image
CREATE INDEX idx_product_image_product_id ON product_image(product_id);

-- =============================================
-- Tabela: product_attribute (Atributos dos produtos)
-- =============================================
CREATE TABLE product_attribute (
   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
   product_id UUID NOT NULL REFERENCES product(id) ON DELETE CASCADE,
   name VARCHAR(50) NOT NULL,
   value VARCHAR(100) NOT NULL
);

-- Índice para product_attribute
CREATE INDEX idx_product_attribute_product_id ON product_attribute(product_id);
CREATE INDEX idx_product_attribute_name ON product_attribute(name);

-- =============================================
-- Comentários nas tabelas (documentação)
-- =============================================
COMMENT ON TABLE category IS 'Categorias de produtos com suporte a hierarquia (até 3 níveis)';
COMMENT ON TABLE product IS 'Produtos do catálogo da loja';
COMMENT ON TABLE product_image IS 'Imagens dos produtos (máximo 10 por produto)';
COMMENT ON TABLE product_attribute IS 'Atributos customizados dos produtos (cor, RAM, etc)';
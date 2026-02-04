# 📦 Product Service - TechStore E-Commerce

> Microserviço responsável pelo gerenciamento de produtos e categorias do catálogo da loja.

---

## 📑 Índice

1. [Visão Geral](#visão-geral)
2. [Entidades](#entidades)
3. [Endpoints](#endpoints)
4. [Regras de Negócio](#regras-de-negócio)
5. [Filtros de Busca](#filtros-de-busca)
6. [Fluxos de Operação](#fluxos-de-operação)
7. [Validações](#validações)

---

## Visão Geral

O **Product Service** é responsável por:

- Gerenciar o catálogo de produtos (CRUD completo)
- Gerenciar categorias com suporte a hierarquia (até 3 níveis)
- Gerenciar imagens e atributos dos produtos
- Fornecer endpoints de busca e listagem com filtros avançados
- Garantir integridade e consistência dos dados do catálogo

### Tecnologias

- **Java 21**
- **Spring Boot 3.2+**
- **Spring Data JPA**
- **PostgreSQL**
- **Flyway** (migrations)

---

## Entidades

### Category (Categoria)

| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | UUID | Gerado automaticamente |
| `name` | String | Obrigatório, único dentro do mesmo nível |
| `slug` | String | Gerado do name, único globalmente |
| `description` | String | Opcional |
| `parentId` | UUID | FK para Category (hierarquia) |
| `active` | Boolean | Default: `true` |
| `createdAt` | DateTime | Gerado automaticamente |

**Características:**
- Suporta hierarquia de até **3 níveis** (ex: Eletrônicos > Smartphones > Apple)
- Slug único globalmente (mesmo que nomes iguais em níveis diferentes)
- Categoria inativa não aparece em listagens públicas

---

### Product (Produto)

| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | UUID | Gerado automaticamente |
| `sku` | String | **Único**, obrigatório |
| `name` | String | Obrigatório, 3-200 caracteres |
| `slug` | String | Gerado do name, **único** |
| `description` | Text | Opcional |
| `brand` | String | Obrigatório |
| `categoryId` | UUID | FK para Category (obrigatório) |
| `price` | Decimal | Obrigatório, **mínimo R$ 0.01** |
| `compareAtPrice` | Decimal | Opcional, preço "de" |
| `active` | Boolean | Default: `true` |
| `createdAt` | DateTime | Gerado automaticamente |
| `updatedAt` | DateTime | Atualizado a cada modificação |

**Características:**
- SKU deve ser único no sistema
- Slug gerado automaticamente do nome
- Produto inativo não aparece em listagens públicas
- Soft delete (não é deletado, apenas `active = false`)

---

### ProductImage (Imagem do Produto)

| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | UUID | Gerado automaticamente |
| `productId` | UUID | FK para Product |
| `url` | String | URL da imagem |
| `altText` | String | Texto alternativo |
| `position` | Integer | Ordem de exibição (0, 1, 2...) |
| `isMain` | Boolean | Se é a imagem principal |

**Características:**
- Máximo de **10 imagens** por produto
- Deve haver **exatamente uma imagem principal** (`isMain = true`)
- Primeira imagem automaticamente se torna principal
- Ao deletar imagem principal, próxima imagem (menor position) vira principal

---

### ProductAttribute (Atributo do Produto)

| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | UUID | Gerado automaticamente |
| `productId` | UUID | FK para Product |
| `name` | String | Nome do atributo (ex: "Cor", "RAM") |
| `value` | String | Valor (ex: "Preto", "8GB") |

**Exemplos:**
- `name: "Cor"`, `value: "Preto"`
- `name: "RAM"`, `value: "8GB"`
- `name: "Armazenamento"`, `value: "256GB"`

---

## Endpoints

### Produtos

| Endpoint | Método | Descrição | Autenticação |
|----------|--------|-----------|--------------|
| `/api/v1/products` | GET | Listar produtos (paginado, com filtros) | Público |
| `/api/v1/products/{id}` | GET | Detalhes do produto | Público |
| `/api/v1/products/slug/{slug}` | GET | Buscar por slug | Público |
| `/api/v1/products/search` | GET | Busca textual | Público |
| `/api/v1/products` | POST | Criar produto | ADMIN |
| `/api/v1/products/{id}` | PUT | Atualizar produto | ADMIN |
| `/api/v1/products/{id}` | DELETE | Desativar produto | ADMIN |
| `/api/v1/products/{id}/images` | POST | Adicionar imagem | ADMIN |
| `/api/v1/products/{id}/images/{imageId}` | DELETE | Remover imagem | ADMIN |
| `/api/v1/products/{id}/attributes` | POST | Adicionar atributo | ADMIN |

### Categorias

| Endpoint | Método | Descrição | Autenticação |
|----------|--------|-----------|--------------|
| `/api/v1/categories` | GET | Listar categorias | Público |
| `/api/v1/categories/{id}` | GET | Detalhes da categoria | Público |
| `/api/v1/categories` | POST | Criar categoria | ADMIN |
| `/api/v1/categories/{id}` | PATCH | Atualizar categoria (parcial) | ADMIN |
| `/api/v1/categories/{id}` | PUT | Atualizar categoria | ADMIN |
| `/api/v1/categories/{id}` | DELETE | Desativar categoria | ADMIN |

---

## Regras de Negócio

### Produtos

| Regra | Comportamento |
|-------|---------------|
| **SKU único** | Não pode existir dois produtos com mesmo SKU |
| **Slug único** | Gerado automaticamente do nome, se já existir adiciona sufixo numérico |
| **Geração de Slug** | Remove acentos, converte para minúsculas, substitui espaços por hífen |
| **Preço mínimo** | R$ 0.01 |
| **compareAtPrice** | Se informado, deve ser **MAIOR** que `price` |
| **Produto inativo** | Não aparece em listagens públicas |
| **Soft Delete** | Produto não é deletado, apenas `active = false` |
| **Categoria obrigatória** | Produto deve pertencer a uma categoria **ativa** |

#### Exemplo de Geração de Slug

```
Nome: "iPhone 15 Pro Max 256GB"
Slug gerado: "iphone-15-pro-max-256gb"

Se já existir:
Slug gerado: "iphone-15-pro-max-256gb-2"
Slug gerado: "iphone-15-pro-max-256gb-3"
...
```

---

### Categorias

| Regra | Comportamento |
|-------|---------------|
| **Hierarquia máxima** | 3 níveis (ex: Eletrônicos > Smartphones > Apple) |
| **Slug único global** | Mesmo que nomes iguais em níveis diferentes |
| **Nome único por nível** | Dentro do mesmo pai não pode repetir (inclui inativas) |
| **Categoria inativa** | Não aparece em listagens, produtos dela também não aparecem |
| **Deletar categoria com produtos** | Não permitido, deve mover produtos primeiro |
| **Deletar categoria com subcategorias** | Não permitido, deve deletar subcategorias primeiro |

#### Criação de Categoria (fluxo e slug)

```
1. Recebe: name, description (opcional), parentId (opcional)
2. Validações:
   - name obrigatório
   - Se parentId informado: categoria pai deve existir e estar ativa
   - Hierarquia máxima: 3 níveis
3. Gera slug a partir do name:
   - Remove acentos
   - Converte para minúsculas
   - Substitui espaços por hífen
4. Garante unicidade global do slug:
   - Se já existir, adiciona sufixo numérico: "-2", "-3", ...
5. Salva categoria com active = true (padrão)
```

**Exemplos de slug:**
- "Smartphones" → `smartphones`
- Se já existir: `smartphones-2`
- "Áudio e Vídeo" → `audio-e-video`

---

### Imagens

| Regra | Comportamento |
|-------|---------------|
| **Imagem principal obrigatória** | Deve haver exatamente uma imagem com `isMain = true` |
| **Primeira imagem** | Automaticamente se torna principal |
| **Definir nova principal** | A anterior perde o status |
| **Deletar imagem principal** | Próxima imagem (menor position) vira principal |
| **Máximo de imagens** | 10 por produto |

---

## Filtros de Busca

### GET `/api/v1/products`

| Parâmetro | Tipo | Descrição | Default |
|-----------|------|-----------|---------|
| `categoryId` | UUID | Filtra por categoria (inclui subcategorias) | - |
| `minPrice` | Decimal | Preço mínimo | - |
| `maxPrice` | Decimal | Preço máximo | - |
| `brand` | String | Filtra por marca | - |
| `search` | String | Busca no nome e descrição | - |
| `active` | Boolean | Filtrar por ativos/inativos | `true` (público) |
| `sortBy` | String | Campo: `"price"`, `"name"`, `"createdAt"`, `"relevance"` | `"createdAt"` |
| `sortDirection` | String | `"asc"` ou `"desc"` | `"desc"` |
| `page` | Integer | Número da página (0-indexed) | `0` |
| `size` | Integer | Itens por página | `20` (max: `100`) |

**Exemplo de requisição:**
```
GET /api/v1/products?categoryId=xxx&minPrice=100&maxPrice=5000&brand=Apple&page=0&size=20&sortBy=price&sortDirection=asc
```

---

### GET `/api/v1/products/search`

| Parâmetro | Tipo | Descrição | Default |
|-----------|------|-----------|---------|
| `q` | String | Texto de busca (obrigatório) | - |
| `page` | Integer | Número da página | `0` |
| `size` | Integer | Itens por página | `20` (max: `100`) |
| `sortBy` | String | Campo de ordenação | `"createdAt"` |
| `sortDirection` | String | `"asc"` ou `"desc"` | `"desc"` |

**Exemplo de requisição:**
```
GET /api/v1/products/search?q=iphone&page=0&size=20
```

---

## Fluxos de Operação

### Criar Produto

```
1. Recebe: sku, name, description, brand, categoryId, price, compareAtPrice, images, attributes

2. Validações:
   - SKU não existe
   - Categoria existe e está ativa
   - Preço >= 0.01
   - Se compareAtPrice informado: compareAtPrice > price

3. Gera slug do nome
   - Se slug existe: adiciona sufixo (-2, -3, etc)

4. Salva produto

5. Se imagens informadas:
   - Primeira imagem: isMain = true
   - Salva todas com position sequencial

6. Se atributos informados:
   - Salva todos os atributos

7. Cria registro no inventory-service com quantidade 0

8. Retorna produto completo
```

---

### Busca com Filtros

```
1. Monta query base: WHERE active = true

2. Se categoryId:
   - Busca categoria e todas subcategorias (recursivo)
   - Adiciona: AND categoryId IN (ids)

3. Se minPrice: AND price >= minPrice

4. Se maxPrice: AND price <= maxPrice

5. Se brand: AND brand ILIKE '%brand%'

6. Se search: AND (name ILIKE '%search%' OR description ILIKE '%search%')

7. Aplica ordenação

8. Aplica paginação

9. Retorna lista paginada
```

---

### Atualizar Produto

```
1. Recebe: id, dados para atualizar

2. Validações:
   - Produto existe
   - Se SKU alterado: novo SKU não existe
   - Se categoria alterada: nova categoria existe e está ativa
   - Se preço alterado: preço >= 0.01
   - Se compareAtPrice informado: compareAtPrice > price

3. Se nome alterado:
   - Re-gera slug
   - Verifica se novo slug é único

4. Atualiza campos

5. Salva produto

6. Retorna produto atualizado
```

---

### Atualizar Categoria (PATCH)

```
1. Recebe: id + campos opcionais (name, description, parentId, active)
2. Validações:
   - Se name informado: não pode ser vazio
   - Se parentId informado: categoria pai deve existir e estar ativa
   - Hierarquia máxima: 3 níveis
   - Nome único por nível (considera ativas e inativas)
3. Se name alterado:
   - Regera slug (mesma regra de criação)
4. Atualiza somente os campos informados
5. Retorna categoria atualizada
```

---

### Desativar Produto (Soft Delete)

```
1. Recebe: id

2. Validações:
   - Produto existe

3. Define active = false

4. Salva produto

5. Retorna sucesso
```

---

## Validações

### Validações de Produto

| Campo | Validação |
|-------|-----------|
| `sku` | Obrigatório, único, não pode ser vazio |
| `name` | Obrigatório, 3-200 caracteres |
| `brand` | Obrigatório, não pode ser vazio |
| `categoryId` | Obrigatório, categoria deve existir e estar ativa |
| `price` | Obrigatório, mínimo R$ 0.01 |
| `compareAtPrice` | Opcional, se informado deve ser > `price` |

### Validações de Categoria

| Campo | Validação |
|-------|-----------|
| `name` | Obrigatório, único dentro do mesmo nível |
| `parentId` | Opcional, se informado deve existir e estar ativo |
| Hierarquia | Máximo 3 níveis |

### Validações de Imagem

| Regra | Validação |
|-------|-----------|
| Máximo | 10 imagens por produto |
| Principal | Deve haver exatamente 1 imagem principal |
| URL | Obrigatória, formato válido |

---

## Códigos de Resposta HTTP

| Código | Significado | Quando usar |
|--------|-------------|-------------|
| `200` | OK | Sucesso em GET, PUT |
| `201` | Created | Sucesso em POST (criação) |
| `204` | No Content | Sucesso em DELETE |
| `400` | Bad Request | Dados inválidos, regra de negócio violada |
| `401` | Unauthorized | Token ausente ou inválido |
| `403` | Forbidden | Token válido mas sem permissão (não é ADMIN) |
| `404` | Not Found | Recurso não existe |
| `409` | Conflict | Conflito (SKU já existe, slug já existe) |
| `422` | Unprocessable Entity | Validação falhou |
| `500` | Internal Server Error | Erro interno do servidor |

---

## Exemplos de Requisições

### Criar Produto

```http
POST /api/v1/products
Content-Type: application/json

{
  "sku": "IPHONE15-256GB-PRETO",
  "name": "iPhone 15 Pro Max 256GB",
  "description": "Smartphone Apple com chip A17 Pro",
  "brand": "Apple",
  "categoryId": "uuid-da-categoria",
  "price": 8999.99,
  "compareAtPrice": 9999.99,
  "images": [
    {
      "url": "https://example.com/image1.jpg",
      "altText": "iPhone 15 Pro Max frontal",
      "position": 0
    }
  ],
  "attributes": [
    {
      "name": "Cor",
      "value": "Preto"
    },
    {
      "name": "Armazenamento",
      "value": "256GB"
    }
  ]
}
```

### Buscar Produtos com Filtros

```http
GET /api/v1/products?categoryId=xxx&minPrice=100&maxPrice=5000&brand=Apple&page=0&size=20&sortBy=price&sortDirection=asc
```

### Busca Textual

```http
GET /api/v1/products/search?q=iphone&page=0&size=20
```

---

## Observações Importantes

1. **Soft Delete**: Produtos e categorias nunca são deletados fisicamente, apenas marcados como inativos (`active = false`)

2. **Hierarquia de Categorias**: Suporta até 3 níveis. Exemplo:
   - Nível 1: Eletrônicos
   - Nível 2: Smartphones
   - Nível 3: Apple

3. **Slug**: Gerado automaticamente e único. Se já existir, adiciona sufixo numérico (-2, -3, etc)

4. **Produtos Inativos**: Não aparecem em listagens públicas, apenas admins podem vê-los

5. **Imagem Principal**: Sempre deve haver exatamente uma imagem principal por produto

6. **Integração com Inventory**: Ao criar produto, deve criar registro no inventory-service com quantidade 0

---

> **Última atualização:** Janeiro 2026
> 
> Este documento deve ser atualizado sempre que houver mudanças nas regras de negócio.

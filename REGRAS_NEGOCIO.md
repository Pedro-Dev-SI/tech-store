# 📋 REGRAS DE NEGÓCIO - TechStore E-Commerce

> Documento completo com todas as regras de negócio, fluxos, validações e comportamentos do sistema.

---

## 📑 Índice

1. [Visão Geral](#visão-geral)
2. [Auth Service](#1-auth-service---autenticação)
3. [User Service](#2-user-service---usuários)
4. [Product Service](#3-product-service---produtos)
5. [Inventory Service](#4-inventory-service---estoque)
6. [Order Service](#5-order-service---pedidos)
7. [Payment Service](#6-payment-service---pagamentos)
8. [Notification Service](#7-notification-service---notificações)
9. [API Gateway](#8-api-gateway)
10. [Fluxos Completos](#fluxos-completos-do-sistema)
11. [Eventos Kafka](#eventos-kafka)

---

## Visão Geral

### Sobre o Sistema
- E-commerce de dispositivos eletrônicos (smartphones, notebooks, tablets, acessórios)
- Arquitetura de microserviços
- Cada serviço possui seu próprio banco de dados (isolamento total)

### Roles (Papéis) do Sistema
| Role | Descrição | Permissões |
|------|-----------|------------|
| `USER` | Cliente comum | Comprar, ver próprio perfil, gerenciar endereços, ver próprios pedidos |
| `ADMIN` | Administrador | Tudo que USER faz + gerenciar produtos, categorias, ver todos usuários/pedidos, atualizar estoque |

### Comunicação entre Serviços
| Tipo | Quando usar | Exemplo |
|------|-------------|---------|
| **Síncrona (REST/OpenFeign)** | Precisa de resposta imediata | Order → Inventory (verificar estoque) |
| **Assíncrona (Kafka)** | Eventos, notificações, processos demorados | Payment aprovado → Notificar usuário |

---

## 1. AUTH-SERVICE - Autenticação

### Entidades

#### RefreshToken
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| token | String | Token de refresh (256 caracteres aleatórios) |
| userId | UUID | ID do usuário dono do token |
| expiryDate | DateTime | Data de expiração |
| revoked | Boolean | Se foi revogado (logout) |

### Endpoints

| Endpoint | Método | Descrição | Autenticação |
|----------|--------|-----------|--------------|
| `/api/v1/auth/register` | POST | Registrar novo usuário | Público |
| `/api/v1/auth/login` | POST | Fazer login | Público |
| `/api/v1/auth/refresh` | POST | Renovar access token | Público (com refresh token) |
| `/api/v1/auth/logout` | POST | Fazer logout | Autenticado |
| `/api/v1/auth/validate` | GET | Validar token (usado pelo Gateway) | Interno |

### Regras de Negócio

#### Tokens
| Regra | Valor | Comportamento |
|-------|-------|---------------|
| Expiração do Access Token | 15 minutos | Após expirar, usuário deve usar refresh token |
| Expiração do Refresh Token | 7 dias | Após expirar, usuário deve fazer login novamente |
| Máximo de Refresh Tokens por usuário | 5 | Ao criar o 6º, o mais antigo é automaticamente revogado |

#### Senha
| Regra | Validação |
|-------|-----------|
| Tamanho mínimo | 8 caracteres |
| Deve conter | Pelo menos 1 letra maiúscula |
| Deve conter | Pelo menos 1 número |
| Armazenamento | Hash BCrypt (nunca texto puro) |

#### Bloqueio por Tentativas Falhas
| Regra | Valor |
|-------|-------|
| Tentativas máximas de login falho | 5 |
| Tempo de bloqueio | 15 minutos |
| Contagem resetada após | Login bem-sucedido |

### Fluxos

#### Registro
```
1. Recebe: email, password, name, cpf, phone
2. Valida formato do email
3. Valida força da senha
4. Chama user-service para criar usuário
5. Se user-service retornar sucesso:
   - Gera access token (JWT)
   - Gera refresh token
   - Salva refresh token no banco
   - Retorna ambos os tokens
6. Se user-service retornar erro:
   - Propaga o erro (email já existe, CPF inválido, etc.)
```

#### Login
```
1. Recebe: email, password
2. Verifica se usuário está bloqueado por tentativas falhas
   - Se bloqueado: retorna erro 423 (Locked) com tempo restante
3. Chama user-service para buscar usuário por email
4. Valida senha com BCrypt
5. Se senha inválida:
   - Incrementa contador de tentativas falhas
   - Se atingiu 5: bloqueia por 15 minutos
   - Retorna erro 401 (Unauthorized)
6. Se senha válida:
   - Reseta contador de tentativas falhas
   - Gera access token
   - Gera refresh token
   - Verifica quantidade de refresh tokens ativos
     - Se >= 5: revoga o mais antigo
   - Salva novo refresh token
   - Retorna ambos os tokens
```

#### Refresh Token
```
1. Recebe: refresh token
2. Busca token no banco
3. Validações:
   - Token existe? Se não: erro 401
   - Token está revogado? Se sim: erro 401
   - Token expirou? Se sim: erro 401
4. Se válido:
   - Gera novo access token
   - Retorna novo access token (refresh token permanece o mesmo)
```

#### Logout
```
1. Recebe: refresh token (no body ou header)
2. Busca token no banco
3. Marca como revogado (revoked = true)
4. Retorna sucesso
```

### Estrutura do JWT (Access Token)
```json
{
  "sub": "user-uuid",
  "email": "user@email.com",
  "role": "USER",
  "iat": 1234567890,
  "exp": 1234568790
}
```

---

## 2. USER-SERVICE - Usuários

### Entidades

#### User
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| email | String | Único, formato válido de email |
| password | String | Hash BCrypt |
| name | String | Obrigatório, 2-100 caracteres |
| cpf | String | Único, válido (algoritmo de validação) |
| phone | String | Formato: (XX) XXXXX-XXXX |
| role | Enum | USER ou ADMIN (default: USER) |
| status | Enum | ACTIVE, INACTIVE, BLOCKED |
| createdAt | DateTime | Gerado automaticamente |
| updatedAt | DateTime | Atualizado a cada modificação |

#### Address
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| userId | UUID | FK para User |
| street | String | Obrigatório |
| number | String | Obrigatório |
| complement | String | Opcional |
| neighborhood | String | Obrigatório |
| city | String | Obrigatório |
| state | String | 2 caracteres (UF) |
| zipCode | String | Formato: XXXXX-XXX |
| isDefault | Boolean | Se é o endereço padrão |

### Endpoints

| Endpoint | Método | Descrição | Autenticação |
|----------|--------|-----------|--------------|
| `/api/v1/users` | POST | Criar usuário | Público |
| `/api/v1/users/me` | GET | Meu perfil | USER |
| `/api/v1/users/me` | PUT | Atualizar meu perfil | USER |
| `/api/v1/users/{id}` | GET | Buscar usuário por ID | ADMIN |
| `/api/v1/users` | GET | Listar todos usuários | ADMIN |
| `/api/v1/users/{id}` | DELETE | Desativar usuário | ADMIN |
| `/api/v1/users/me/addresses` | GET | Listar meus endereços | USER |
| `/api/v1/users/me/addresses` | POST | Adicionar endereço | USER |
| `/api/v1/users/me/addresses/{id}` | PUT | Atualizar endereço | USER |
| `/api/v1/users/me/addresses/{id}` | DELETE | Remover endereço | USER |
| `/api/v1/users/email/{email}` | GET | Buscar por email (interno) | Interno |

### Regras de Negócio

#### Usuário
| Regra | Comportamento |
|-------|---------------|
| Email único | Não pode existir dois usuários com mesmo email |
| CPF único | Não pode existir dois usuários com mesmo CPF |
| CPF válido | Deve passar no algoritmo de validação de CPF |
| Soft Delete | Usuário nunca é deletado, apenas status muda para INACTIVE |
| Usuário INACTIVE | Não pode fazer login |
| Usuário BLOCKED | Não pode fazer login (bloqueado por admin) |

#### Validação de CPF (Algoritmo)
```
1. Remove caracteres não numéricos
2. Verifica se tem 11 dígitos
3. Verifica se não são todos iguais (111.111.111-11 é inválido)
4. Calcula primeiro dígito verificador
5. Calcula segundo dígito verificador
6. Compara com os dígitos informados
```

#### Endereços
| Regra | Comportamento |
|-------|---------------|
| Máximo por usuário | 5 endereços |
| Endereço padrão obrigatório | Sempre deve haver exatamente 1 endereço com isDefault = true |
| Primeiro endereço | Automaticamente se torna o padrão |
| Definir novo padrão | O anterior perde o status de padrão |
| Deletar endereço padrão | Próximo endereço mais antigo vira padrão (se houver) |
| Deletar único endereço | Permitido, usuário fica sem endereço |

### Fluxos

#### Criar Usuário
```
1. Recebe: email, password, name, cpf, phone
2. Validações:
   - Email formato válido
   - Email não existe no banco
   - CPF formato válido (XXX.XXX.XXX-XX ou só números)
   - CPF passa no algoritmo de validação
   - CPF não existe no banco
   - Nome entre 2-100 caracteres
   - Telefone formato válido
3. Faz hash da senha com BCrypt
4. Salva usuário com:
   - role = USER
   - status = ACTIVE
   - createdAt = agora
5. Publica evento: user.registered
6. Retorna usuário (sem senha)
```

#### Adicionar Endereço
```
1. Recebe: dados do endereço
2. Validações:
   - Usuário autenticado existe
   - Usuário tem menos de 5 endereços
   - CEP formato válido
   - Estado é UF válida
3. Se é o primeiro endereço:
   - Define isDefault = true
4. Se isDefault = true no request:
   - Remove isDefault dos outros endereços
5. Salva endereço
6. Retorna endereço criado
```

#### Desativar Usuário (Admin)
```
1. Recebe: userId
2. Validações:
   - Usuário existe
   - Usuário não é o próprio admin fazendo a requisição
   - Usuário não é outro ADMIN (admin não pode desativar admin)
3. Atualiza status para INACTIVE
4. Revoga todos os refresh tokens do usuário (chama auth-service)
5. Retorna sucesso
```

---

## 3. PRODUCT-SERVICE - Produtos

### Entidades

#### Category
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| name | String | Obrigatório, único dentro do mesmo nível |
| slug | String | Gerado do name, único globalmente |
| description | String | Opcional |
| parentId | UUID | FK para Category (hierarquia) |
| active | Boolean | Default: true |
| createdAt | DateTime | Gerado automaticamente |

#### Product
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| sku | String | Único, obrigatório |
| name | String | Obrigatório, 3-200 caracteres |
| slug | String | Gerado do name, único |
| description | Text | Opcional |
| brand | String | Obrigatório |
| categoryId | UUID | FK para Category |
| price | Decimal | Obrigatório, mínimo 0.01 |
| compareAtPrice | Decimal | Opcional, preço "de" |
| active | Boolean | Default: true |
| createdAt | DateTime | Gerado automaticamente |
| updatedAt | DateTime | Atualizado a cada modificação |

#### ProductImage
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| productId | UUID | FK para Product |
| url | String | URL da imagem |
| altText | String | Texto alternativo |
| position | Integer | Ordem de exibição (0, 1, 2...) |
| isMain | Boolean | Se é a imagem principal |

#### ProductAttribute
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| productId | UUID | FK para Product |
| name | String | Nome do atributo (ex: "Cor", "RAM") |
| value | String | Valor (ex: "Preto", "8GB") |

### Endpoints

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
| `/api/v1/categories` | GET | Listar categorias | Público |
| `/api/v1/categories/{id}` | GET | Detalhes da categoria | Público |
| `/api/v1/categories` | POST | Criar categoria | ADMIN |
| `/api/v1/categories/{id}` | PUT | Atualizar categoria | ADMIN |
| `/api/v1/categories/{id}` | DELETE | Desativar categoria | ADMIN |

### Regras de Negócio

#### Produtos
| Regra | Comportamento |
|-------|---------------|
| SKU único | Não pode existir dois produtos com mesmo SKU |
| Slug único | Gerado automaticamente do nome, se já existir adiciona sufixo numérico |
| Geração de Slug | Remove acentos, converte para minúsculas, substitui espaços por hífen |
| Preço mínimo | R$ 0.01 |
| compareAtPrice | Se informado, deve ser MAIOR que price |
| Produto inativo | Não aparece em listagens públicas |
| Soft Delete | Produto não é deletado, apenas active = false |
| Categoria obrigatória | Produto deve pertencer a uma categoria ativa |

#### Exemplo de Geração de Slug
```
Nome: "iPhone 15 Pro Max 256GB"
Slug: "iphone-15-pro-max-256gb"

Se já existir:
Slug: "iphone-15-pro-max-256gb-2"
```

#### Categorias
| Regra | Comportamento |
|-------|---------------|
| Hierarquia máxima | 3 níveis (ex: Eletrônicos > Smartphones > Apple) |
| Slug único global | Mesmo que nomes iguais em níveis diferentes |
| Categoria inativa | Não aparece em listagens, produtos dela também não aparecem |
| Deletar categoria com produtos | Não permitido, deve mover produtos primeiro |
| Deletar categoria com subcategorias | Não permitido, deve deletar subcategorias primeiro |

#### Imagens
| Regra | Comportamento |
|-------|---------------|
| Imagem principal obrigatória | Deve haver exatamente uma imagem com isMain = true |
| Primeira imagem | Automaticamente se torna principal |
| Definir nova principal | A anterior perde o status |
| Deletar imagem principal | Próxima imagem (menor position) vira principal |
| Máximo de imagens | 10 por produto |

### Filtros de Busca (GET /api/v1/products)

| Parâmetro | Tipo | Descrição |
|-----------|------|-----------|
| categoryId | UUID | Filtra por categoria (inclui subcategorias) |
| minPrice | Decimal | Preço mínimo |
| maxPrice | Decimal | Preço máximo |
| brand | String | Filtra por marca |
| search | String | Busca no nome e descrição |
| active | Boolean | Default true para público, admin pode ver inativos |
| sortBy | String | "price", "name", "createdAt", "relevance" |
| sortDirection | String | "asc" ou "desc" |
| page | Integer | Página (default 0) |
| size | Integer | Itens por página (default 20, max 100) |

### Fluxos

#### Criar Produto
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

#### Busca com Filtros
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

## 4. INVENTORY-SERVICE - Estoque

### Entidades

#### Inventory
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| productId | UUID | Único, referência ao produto |
| quantity | Integer | Quantidade total em estoque |
| reservedQuantity | Integer | Quantidade reservada para pedidos |
| minStockAlert | Integer | Quantidade mínima para alerta |
| updatedAt | DateTime | Última atualização |

#### StockMovement
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| inventoryId | UUID | FK para Inventory |
| type | Enum | IN, OUT, RESERVE, RELEASE |
| quantity | Integer | Quantidade movimentada |
| reason | String | Motivo da movimentação |
| orderId | UUID | ID do pedido (se aplicável) |
| createdAt | DateTime | Data da movimentação |
| createdBy | UUID | Usuário que fez a movimentação |

### Tipos de Movimentação
| Tipo | Descrição | Efeito em quantity | Efeito em reservedQuantity |
|------|-----------|-------------------|---------------------------|
| IN | Entrada de estoque | +quantity | - |
| OUT | Saída confirmada (venda) | -quantity | -quantity |
| RESERVE | Reserva para pedido | - | +quantity |
| RELEASE | Liberação de reserva | - | -quantity |

### Endpoints

| Endpoint | Método | Descrição | Autenticação |
|----------|--------|-----------|--------------|
| `/api/v1/inventory/{productId}` | GET | Consultar estoque | Interno/ADMIN |
| `/api/v1/inventory/{productId}` | PUT | Atualizar quantidade | ADMIN |
| `/api/v1/inventory/reserve` | POST | Reservar estoque | Interno |
| `/api/v1/inventory/release` | POST | Liberar reserva | Interno |
| `/api/v1/inventory/confirm` | POST | Confirmar baixa | Interno |
| `/api/v1/inventory/low-stock` | GET | Produtos com estoque baixo | ADMIN |
| `/api/v1/inventory/movements/{productId}` | GET | Histórico de movimentações | ADMIN |

### Regras de Negócio

#### Cálculos
| Cálculo | Fórmula |
|---------|---------|
| Quantidade Disponível | quantity - reservedQuantity |
| Estoque Baixo | quantity <= minStockAlert |

#### Reserva de Estoque
| Regra | Comportamento |
|-------|---------------|
| Validação | Só reserva se quantidade disponível >= quantidade solicitada |
| Timeout | Reserva expira em 30 minutos se pedido não for pago |
| Múltiplos itens | Reserva é atômica - ou reserva todos ou nenhum |

#### Alertas
| Regra | Comportamento |
|-------|---------------|
| Estoque baixo | Quando quantity <= minStockAlert, publica evento stock.low-alert |
| Frequência do alerta | Máximo 1 alerta por produto a cada 24 horas |

#### Auditoria
| Regra | Comportamento |
|-------|---------------|
| Toda movimentação | Gera registro em StockMovement |
| Campos obrigatórios | type, quantity, reason |
| Imutável | Registros de movimento nunca são alterados ou deletados |

### Fluxos

#### Reservar Estoque (para pedido)
```
1. Recebe: lista de { productId, quantity }, orderId
2. Inicia transação
3. Para cada item:
   - Busca inventory pelo productId (com lock FOR UPDATE)
   - Calcula disponível = quantity - reservedQuantity
   - Se disponível < quantidade solicitada:
     - Rollback
     - Retorna erro com produto e quantidade disponível
4. Se todos disponíveis:
   - Para cada item:
     - Incrementa reservedQuantity
     - Cria StockMovement tipo RESERVE
   - Agenda job para liberar reserva em 30 minutos
5. Commit
6. Retorna sucesso
```

#### Liberar Reserva (timeout ou cancelamento)
```
1. Recebe: orderId
2. Busca todas as reservas do pedido (StockMovements tipo RESERVE)
3. Para cada reserva:
   - Busca inventory
   - Decrementa reservedQuantity
   - Cria StockMovement tipo RELEASE
4. Publica evento: stock.released
5. Retorna sucesso
```

#### Confirmar Baixa (pagamento aprovado)
```
1. Recebe: orderId
2. Busca todas as reservas do pedido
3. Para cada reserva:
   - Busca inventory
   - Decrementa quantity (saída definitiva)
   - Decrementa reservedQuantity (já não está mais reservado)
   - Cria StockMovement tipo OUT
   - Se quantity <= minStockAlert:
     - Publica evento stock.low-alert
4. Publica evento: stock.confirmed
5. Retorna sucesso
```

### Eventos Kafka

| Evento | Quando | Payload |
|--------|--------|---------|
| stock.reserved | Estoque reservado | orderId, items[] |
| stock.released | Reserva liberada | orderId, reason |
| stock.confirmed | Baixa confirmada | orderId, items[] |
| stock.low-alert | Estoque baixo | productId, productName, currentQuantity, minStockAlert |

---

## 5. ORDER-SERVICE - Pedidos

### Entidades

#### Order
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| orderNumber | String | Único, formato: TS-YYYYMMDD-XXXXX |
| userId | UUID | ID do usuário que fez o pedido |
| status | Enum | Status atual do pedido |
| totalAmount | Decimal | Valor total do pedido |
| shippingAddress | JSON | Endereço de entrega (snapshot) |
| notes | String | Observações do cliente |
| createdAt | DateTime | Data de criação |
| updatedAt | DateTime | Última atualização |

#### OrderItem
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| orderId | UUID | FK para Order |
| productId | UUID | ID do produto |
| productName | String | Nome do produto (snapshot) |
| productSku | String | SKU do produto (snapshot) |
| quantity | Integer | Quantidade |
| unitPrice | Decimal | Preço unitário no momento da compra |
| totalPrice | Decimal | quantity * unitPrice |

#### OrderStatusHistory
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| orderId | UUID | FK para Order |
| fromStatus | Enum | Status anterior (null se primeiro) |
| toStatus | Enum | Novo status |
| notes | String | Observações da mudança |
| createdAt | DateTime | Data da mudança |
| createdBy | UUID | Quem fez a mudança |

### Status do Pedido

```
                    ┌─────────────────┐
                    │ PENDING_PAYMENT │ (Aguardando pagamento)
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              │              ▼
    ┌─────────────────┐      │    ┌─────────────────┐
    │ PAYMENT_FAILED  │      │    │   CANCELLED     │
    └─────────────────┘      │    └─────────────────┘
                             │
                             ▼
                    ┌─────────────────────┐
                    │ PAYMENT_CONFIRMED   │ (Pagamento confirmado)
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────┐
              │                │            │
              ▼                │            ▼
    ┌─────────────────┐        │    ┌─────────────────┐
    │   CANCELLED     │        │    │   REFUNDED      │
    └─────────────────┘        │    └─────────────────┘
                               │
                               ▼
                    ┌─────────────────┐
                    │   PROCESSING    │ (Em preparação)
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              │              ▼
    ┌─────────────────┐      │    ┌─────────────────┐
    │   CANCELLED     │      │    │   REFUNDED      │
    └─────────────────┘      │    └─────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │    SHIPPED      │ (Enviado)
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │   DELIVERED     │ (Entregue)
                    └─────────────────┘
```

### Transições Permitidas

| De | Para | Quem pode | Condições |
|----|------|-----------|-----------|
| PENDING_PAYMENT | PAYMENT_CONFIRMED | Sistema | Pagamento aprovado |
| PENDING_PAYMENT | PAYMENT_FAILED | Sistema | Pagamento falhou |
| PENDING_PAYMENT | CANCELLED | USER/ADMIN/Sistema | Cancelamento ou timeout 30min |
| PAYMENT_CONFIRMED | PROCESSING | ADMIN | Início da preparação |
| PAYMENT_CONFIRMED | CANCELLED | ADMIN | Cancelamento pelo admin |
| PAYMENT_CONFIRMED | REFUNDED | Sistema | Reembolso processado |
| PROCESSING | SHIPPED | ADMIN | Pedido enviado |
| PROCESSING | CANCELLED | ADMIN | Cancelamento pelo admin |
| PROCESSING | REFUNDED | Sistema | Reembolso processado |
| SHIPPED | DELIVERED | ADMIN/Sistema | Confirmação de entrega |

### Endpoints

| Endpoint | Método | Descrição | Autenticação |
|----------|--------|-----------|--------------|
| `/api/v1/orders` | POST | Criar pedido | USER |
| `/api/v1/orders` | GET | Meus pedidos | USER |
| `/api/v1/orders/{id}` | GET | Detalhes do pedido | USER (próprio) / ADMIN |
| `/api/v1/orders/{id}/cancel` | POST | Cancelar pedido | USER (próprio) / ADMIN |
| `/api/v1/orders/admin` | GET | Todos os pedidos | ADMIN |
| `/api/v1/orders/{id}/status` | PUT | Atualizar status | ADMIN |

### Regras de Negócio

#### Criação de Pedido
| Regra | Comportamento |
|-------|---------------|
| Itens obrigatórios | Pedido deve ter pelo menos 1 item |
| Quantidade mínima | Cada item deve ter quantity >= 1 |
| Endereço obrigatório | Usuário deve ter pelo menos 1 endereço cadastrado |
| Produtos ativos | Todos os produtos devem estar ativos |
| Estoque disponível | Todos os itens devem ter estoque disponível |
| Preço atual | Usa preço atual do produto (não aceita preço do cliente) |
| Snapshot | Salva nome, SKU e preço no momento da compra |

#### Cancelamento
| Regra | Comportamento |
|-------|---------------|
| Usuário pode cancelar | Apenas se status = PENDING_PAYMENT |
| Admin pode cancelar | Se status in (PENDING_PAYMENT, PAYMENT_CONFIRMED, PROCESSING) |
| Após cancelamento | Libera reserva de estoque |
| Se já pago | Inicia processo de reembolso |

#### Timeout
| Regra | Comportamento |
|-------|---------------|
| Tempo para pagamento | 30 minutos |
| Após timeout | Status muda para CANCELLED, estoque liberado |
| Job de verificação | Roda a cada 5 minutos |

### Fluxos

#### Criar Pedido
```
1. Recebe: items[{productId, quantity}], addressId, notes
2. Validações:
   - Usuário autenticado
   - items não está vazio
   - Endereço existe e pertence ao usuário
3. Para cada item:
   - Busca produto no product-service
   - Valida: produto existe e está ativo
   - Calcula totalPrice = quantity * price
4. Chama inventory-service para reservar estoque
   - Se falhar: retorna erro com detalhes
5. Calcula totalAmount (soma dos totalPrice)
6. Cria Order com status PENDING_PAYMENT
7. Cria OrderItems com snapshot dos dados
8. Cria OrderStatusHistory
9. Agenda job de timeout para 30 minutos
10. Publica evento: order.created
11. Retorna pedido criado
```

#### Atualizar Status (Admin)
```
1. Recebe: orderId, newStatus, notes
2. Busca pedido
3. Valida transição permitida (tabela acima)
4. Atualiza status
5. Cria OrderStatusHistory
6. Publica evento correspondente:
   - order.shipped (se SHIPPED)
   - order.delivered (se DELIVERED)
   - order.cancelled (se CANCELLED)
7. Retorna pedido atualizado
```

### Eventos Kafka

| Evento | Quando | Payload |
|--------|--------|---------|
| order.created | Pedido criado | orderId, userId, items[], totalAmount |
| order.cancelled | Pedido cancelado | orderId, reason |
| order.paid | Pagamento confirmado | orderId |
| order.shipped | Pedido enviado | orderId, trackingCode |
| order.delivered | Pedido entregue | orderId |

---

## 6. PAYMENT-SERVICE - Pagamentos

### Entidades

#### Payment
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| orderId | UUID | ID do pedido |
| amount | Decimal | Valor do pagamento |
| method | Enum | Método de pagamento |
| status | Enum | Status do pagamento |
| transactionId | String | ID da transação no gateway |
| gatewayResponse | JSON | Resposta completa do gateway |
| paidAt | DateTime | Data do pagamento (se aprovado) |
| createdAt | DateTime | Data de criação |
| updatedAt | DateTime | Última atualização |

#### PaymentMethod (Enum)
| Valor | Descrição |
|-------|-----------|
| CREDIT_CARD | Cartão de crédito |
| DEBIT_CARD | Cartão de débito |
| PIX | PIX |
| BOLETO | Boleto bancário |

#### PaymentStatus (Enum)
| Valor | Descrição |
|-------|-----------|
| PENDING | Aguardando processamento |
| PROCESSING | Sendo processado pelo gateway |
| APPROVED | Aprovado |
| FAILED | Falhou/Recusado |
| REFUNDED | Reembolsado |
| PARTIALLY_REFUNDED | Parcialmente reembolsado |

### Status do Pagamento

```
    ┌─────────────┐
    │   PENDING   │
    └──────┬──────┘
           │
           ▼
    ┌─────────────┐
    │ PROCESSING  │
    └──────┬──────┘
           │
     ┌─────┴─────┐
     │           │
     ▼           ▼
┌─────────┐ ┌─────────┐
│ APPROVED│ │ FAILED  │
└────┬────┘ └─────────┘
     │
     ▼
┌───────────────────┐
│ REFUNDED /        │
│ PARTIALLY_REFUNDED│
└───────────────────┘
```

### Endpoints

| Endpoint | Método | Descrição | Autenticação |
|----------|--------|-----------|--------------|
| `/api/v1/payments` | POST | Iniciar pagamento | USER |
| `/api/v1/payments/{orderId}` | GET | Status do pagamento | USER (próprio) / ADMIN |
| `/api/v1/payments/webhook` | POST | Webhook do gateway | Público (validado por assinatura) |
| `/api/v1/payments/{id}/refund` | POST | Solicitar reembolso | ADMIN |
| `/api/v1/payments/admin` | GET | Listar pagamentos | ADMIN |

### Regras de Negócio

#### Processamento
| Regra | Comportamento |
|-------|---------------|
| Pedido válido | Só processa se pedido está PENDING_PAYMENT |
| Valor | Deve ser igual ao totalAmount do pedido |
| Tentativas | Máximo 3 tentativas por pedido |
| Timeout | Se gateway não responder em 30s, marca como FAILED |

#### Webhook
| Regra | Comportamento |
|-------|---------------|
| Validação de assinatura | Obrigatório validar HMAC do gateway |
| Idempotência | Mesmo webhook recebido múltiplas vezes não processa novamente |
| Retry | Se processamento falhar, gateway reenvia (até 5 vezes) |

#### Reembolso
| Regra | Comportamento |
|-------|---------------|
| Prazo | Só pagamentos APPROVED com menos de 7 dias |
| Parcial | Permitido especificar valor menor que o total |
| Após reembolso total | Status do pedido muda para REFUNDED |
| Estoque | NÃO retorna automaticamente ao estoque (decisão manual) |

### Fluxos

#### Processar Pagamento
```
1. Recebe: orderId, method, paymentDetails (cartão, etc)
2. Validações:
   - Pedido existe e status = PENDING_PAYMENT
   - Método válido
   - Não existe pagamento APPROVED para este pedido
3. Cria Payment com status PENDING
4. Envia para gateway:
   - Se PIX: gateway retorna QR Code
   - Se CREDIT_CARD: gateway processa imediatamente
   - Se BOLETO: gateway retorna código de barras
5. Atualiza status para PROCESSING
6. Se resposta síncrona (cartão):
   - Se aprovado: status = APPROVED
   - Se recusado: status = FAILED
7. Se resposta assíncrona (PIX, boleto):
   - Retorna dados para pagamento
   - Aguarda webhook
8. Se APPROVED:
   - Atualiza pedido para PAYMENT_CONFIRMED
   - Confirma baixa no estoque
   - Publica evento: payment.approved
9. Se FAILED:
   - Libera reserva de estoque
   - Atualiza pedido para PAYMENT_FAILED
   - Publica evento: payment.failed
10. Retorna resultado
```

#### Webhook do Gateway
```
1. Recebe: payload do gateway
2. Valida assinatura HMAC
   - Se inválida: retorna 401
3. Busca pagamento pelo transactionId
   - Se não encontrar: retorna 404
4. Verifica se já processado (idempotência)
   - Se mesmo status: retorna 200 OK
5. Atualiza status baseado no payload
6. Se APPROVED:
   - Atualiza pedido para PAYMENT_CONFIRMED
   - Confirma baixa no estoque
   - Publica evento: payment.approved
7. Se FAILED:
   - Libera reserva de estoque
   - Atualiza pedido para PAYMENT_FAILED
   - Publica evento: payment.failed
8. Retorna 200 OK
```

#### Reembolso
```
1. Recebe: paymentId, amount (opcional, default = total)
2. Validações:
   - Pagamento existe e status = APPROVED
   - Pagamento tem menos de 7 dias
   - Amount <= valor original
3. Envia requisição de reembolso ao gateway
4. Se aprovado:
   - Se amount = total: status = REFUNDED
   - Se amount < total: status = PARTIALLY_REFUNDED
   - Atualiza pedido para REFUNDED (se total)
   - Publica evento: payment.refunded
5. Retorna resultado
```

### Eventos Kafka

| Evento | Quando | Payload |
|--------|--------|---------|
| payment.approved | Pagamento aprovado | orderId, paymentId, amount, method |
| payment.failed | Pagamento falhou | orderId, paymentId, reason |
| payment.refunded | Reembolso processado | orderId, paymentId, refundAmount |

---

## 7. NOTIFICATION-SERVICE - Notificações

### Entidades

#### NotificationLog
| Campo | Tipo | Regras |
|-------|------|--------|
| id | UUID | Gerado automaticamente |
| userId | UUID | ID do usuário destinatário |
| type | Enum | Tipo da notificação |
| channel | Enum | Canal (EMAIL, SMS, PUSH) |
| recipient | String | Email, telefone, etc |
| subject | String | Assunto (para email) |
| content | Text | Conteúdo da mensagem |
| status | Enum | PENDING, SENT, FAILED |
| errorMessage | String | Mensagem de erro (se falhou) |
| sentAt | DateTime | Data de envio |
| createdAt | DateTime | Data de criação |

### Tipos de Notificação

| Tipo | Evento Kafka | Canal | Template |
|------|--------------|-------|----------|
| WELCOME | user.registered | EMAIL | Bem-vindo à TechStore |
| ORDER_CREATED | order.created | EMAIL | Pedido #{orderNumber} recebido |
| PAYMENT_APPROVED | payment.approved | EMAIL | Pagamento confirmado |
| PAYMENT_FAILED | payment.failed | EMAIL | Problema no pagamento |
| ORDER_SHIPPED | order.shipped | EMAIL | Seu pedido foi enviado |
| ORDER_DELIVERED | order.delivered | EMAIL | Pedido entregue |
| LOW_STOCK_ALERT | stock.low-alert | EMAIL | Alerta de estoque baixo (para admins) |

### Regras de Negócio

#### Processamento
| Regra | Comportamento |
|-------|---------------|
| Retry | Se falhar envio, tenta novamente até 3 vezes com backoff exponencial |
| Backoff | 1 min, 5 min, 15 min |
| Após 3 falhas | Marca como FAILED, não tenta mais |

#### Destinatários
| Tipo | Destinatário |
|------|--------------|
| Notificações de pedido | Usuário que fez o pedido |
| Alertas de estoque | Todos os usuários ADMIN |

#### Templates
| Regra | Comportamento |
|-------|---------------|
| Variáveis | Templates usam placeholders como {{userName}}, {{orderNumber}} |
| HTML | Emails são enviados em HTML com versão texto puro |
| Assunto dinâmico | Pode incluir variáveis (ex: "Pedido #TS-20240101-00001 recebido") |

### Fluxo de Consumo de Eventos

```
1. Consome evento do Kafka
2. Identifica tipo de notificação
3. Busca dados necessários:
   - Usuário (email, nome)
   - Pedido (número, itens, valores)
   - Outros dados relevantes
4. Carrega template correspondente
5. Substitui variáveis no template
6. Cria NotificationLog com status PENDING
7. Envia notificação:
   - EMAIL: via SMTP ou serviço (SendGrid, SES)
8. Se sucesso:
   - Atualiza status = SENT
   - Atualiza sentAt
9. Se falha:
   - Incrementa contador de tentativas
   - Se < 3: agenda retry
   - Se >= 3: status = FAILED, salva errorMessage
```

---

## 8. API-GATEWAY

### Funcionalidades

| Funcionalidade | Descrição |
|----------------|-----------|
| Roteamento | Direciona requisições para o serviço correto |
| Autenticação | Valida JWT em rotas protegidas |
| Rate Limiting | Limita requisições por IP/usuário |
| CORS | Configuração de Cross-Origin |
| Logging | Log de todas as requisições |

### Roteamento

| Path | Serviço | Autenticação |
|------|---------|--------------|
| `/api/v1/auth/**` | auth-service | Público |
| `/api/v1/users/**` | user-service | Variável (ver regras) |
| `/api/v1/products/**` | product-service | GET público, outros ADMIN |
| `/api/v1/categories/**` | product-service | GET público, outros ADMIN |
| `/api/v1/inventory/**` | inventory-service | ADMIN ou Interno |
| `/api/v1/orders/**` | order-service | USER |
| `/api/v1/payments/**` | payment-service | Variável |

### Regras de Autenticação

#### Rotas Públicas (não precisa de JWT)
- POST `/api/v1/auth/register`
- POST `/api/v1/auth/login`
- POST `/api/v1/auth/refresh`
- GET `/api/v1/products/**`
- GET `/api/v1/categories/**`
- POST `/api/v1/payments/webhook`

#### Rotas USER (precisa de JWT com role USER ou ADMIN)
- GET/PUT `/api/v1/users/me`
- GET/POST/PUT/DELETE `/api/v1/users/me/addresses/**`
- POST/GET `/api/v1/orders/**`
- POST/GET `/api/v1/payments/**` (exceto webhook)

#### Rotas ADMIN (precisa de JWT com role ADMIN)
- GET `/api/v1/users/{id}`
- GET `/api/v1/users`
- DELETE `/api/v1/users/{id}`
- POST/PUT/DELETE `/api/v1/products/**`
- POST/PUT/DELETE `/api/v1/categories/**`
- GET/PUT `/api/v1/inventory/**`
- GET `/api/v1/orders/admin`
- PUT `/api/v1/orders/{id}/status`
- POST `/api/v1/payments/{id}/refund`

### Rate Limiting

| Tipo | Limite | Janela |
|------|--------|--------|
| Por IP (não autenticado) | 100 requisições | 1 minuto |
| Por usuário (autenticado) | 1000 requisições | 1 minuto |
| Endpoints de login | 10 requisições | 1 minuto |

### Fluxo de Requisição

```
1. Requisição chega no Gateway
2. Verifica Rate Limiting
   - Se excedeu: retorna 429 Too Many Requests
3. Verifica se rota é pública
   - Se sim: encaminha para serviço
4. Se rota protegida:
   - Extrai JWT do header Authorization
   - Se não tem JWT: retorna 401 Unauthorized
   - Valida JWT (assinatura, expiração)
   - Se inválido: retorna 401 Unauthorized
   - Extrai role do JWT
   - Verifica se role tem permissão
   - Se não tem: retorna 403 Forbidden
5. Adiciona headers internos:
   - X-User-Id: ID do usuário
   - X-User-Role: Role do usuário
6. Encaminha para serviço destino
7. Retorna resposta ao cliente
```

---

## Fluxos Completos do Sistema

### Fluxo de Compra (Sucesso)

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Cliente │    │ Gateway │    │  Order  │    │Inventory│    │ Payment │    │  Notif  │
└────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘
     │              │              │              │              │              │
     │ POST /orders │              │              │              │              │
     │─────────────►│              │              │              │              │
     │              │ Valida JWT   │              │              │              │
     │              │─────────────►│              │              │              │
     │              │              │              │              │              │
     │              │              │ Reserva estoque             │              │
     │              │              │─────────────►│              │              │
     │              │              │              │              │              │
     │              │              │◄─────────────│              │              │
     │              │              │   OK         │              │              │
     │              │              │              │              │              │
     │              │              │ Cria pedido  │              │              │
     │              │              │──────┐       │              │              │
     │              │              │      │       │              │              │
     │              │              │◄─────┘       │              │              │
     │              │              │              │              │              │
     │              │              │ Kafka: order.created ──────────────────────►│
     │              │              │              │              │              │
     │◄─────────────│◄─────────────│              │              │              │
     │ Pedido criado│              │              │              │              │
     │              │              │              │              │              │
     │ POST /payments              │              │              │              │
     │─────────────►│─────────────►│─────────────────────────────►│              │
     │              │              │              │              │              │
     │              │              │              │              │ Processa     │
     │              │              │              │              │──────┐       │
     │              │              │              │              │      │       │
     │              │              │              │              │◄─────┘       │
     │              │              │              │              │              │
     │              │              │              │ Confirma baixa│              │
     │              │              │              │◄──────────────│              │
     │              │              │              │              │              │
     │              │              │ Atualiza status             │              │
     │              │              │◄─────────────────────────────│              │
     │              │              │              │              │              │
     │              │              │              │ Kafka: payment.approved ────►│
     │              │              │              │              │              │
     │◄─────────────│◄────────────────────────────────────────────│              │
     │ Pago!        │              │              │              │     Email    │
     │              │              │              │              │     enviado  │
```

### Fluxo de Compra (Falha - Sem Estoque)

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Cliente │    │ Gateway │    │  Order  │    │Inventory│
└────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘
     │              │              │              │
     │ POST /orders │              │              │
     │─────────────►│─────────────►│              │
     │              │              │              │
     │              │              │ Reserva estoque
     │              │              │─────────────►│
     │              │              │              │
     │              │              │◄─────────────│
     │              │              │ ERRO: Sem estoque
     │              │              │ produto X    │
     │              │              │              │
     │◄─────────────│◄─────────────│              │
     │ 400 Bad Request             │              │
     │ "Produto X sem estoque"     │              │
```

### Fluxo de Cancelamento

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Cliente │    │ Gateway │    │  Order  │    │Inventory│    │  Notif  │
└────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘
     │              │              │              │              │
     │POST /orders/{id}/cancel     │              │              │
     │─────────────►│─────────────►│              │              │
     │              │              │              │              │
     │              │              │ Valida status│              │
     │              │              │ (deve ser PENDING_PAYMENT)  │
     │              │              │              │              │
     │              │              │ Libera reserva              │
     │              │              │─────────────►│              │
     │              │              │              │              │
     │              │              │◄─────────────│              │
     │              │              │   OK         │              │
     │              │              │              │              │
     │              │              │ Kafka: order.cancelled ────►│
     │              │              │              │              │
     │◄─────────────│◄─────────────│              │              │
     │ Cancelado    │              │              │     Email    │
```

---

## Eventos Kafka

### Lista Completa de Eventos

| Evento | Produtor | Consumidores | Descrição |
|--------|----------|--------------|-----------|
| user.registered | user-service | notification-service | Novo usuário cadastrado |
| order.created | order-service | notification-service | Pedido criado |
| order.cancelled | order-service | inventory-service, notification-service | Pedido cancelado |
| order.shipped | order-service | notification-service | Pedido enviado |
| order.delivered | order-service | notification-service | Pedido entregue |
| payment.approved | payment-service | order-service, inventory-service, notification-service | Pagamento aprovado |
| payment.failed | payment-service | order-service, inventory-service, notification-service | Pagamento falhou |
| payment.refunded | payment-service | order-service, notification-service | Reembolso processado |
| stock.reserved | inventory-service | - | Estoque reservado |
| stock.released | inventory-service | - | Reserva liberada |
| stock.confirmed | inventory-service | - | Baixa confirmada |
| stock.low-alert | inventory-service | notification-service | Estoque baixo |

### Estrutura dos Eventos

Todos os eventos seguem a estrutura base:

```json
{
  "eventId": "uuid",
  "eventType": "order.created",
  "timestamp": "2024-01-01T12:00:00Z",
  "payload": {
    // dados específicos do evento
  }
}
```

### Garantias

| Garantia | Configuração |
|----------|--------------|
| Ordenação | Eventos do mesmo aggregate (orderId, userId) vão para mesma partição |
| At-least-once | Consumidores devem ser idempotentes |
| Persistência | Retenção de 7 dias |

---

## Códigos de Erro HTTP

| Código | Significado | Quando usar |
|--------|-------------|-------------|
| 200 | OK | Sucesso em GET, PUT |
| 201 | Created | Sucesso em POST (criação) |
| 204 | No Content | Sucesso em DELETE |
| 400 | Bad Request | Dados inválidos, regra de negócio violada |
| 401 | Unauthorized | Token ausente ou inválido |
| 403 | Forbidden | Token válido mas sem permissão |
| 404 | Not Found | Recurso não existe |
| 409 | Conflict | Conflito (email já existe, etc) |
| 422 | Unprocessable Entity | Validação falhou |
| 423 | Locked | Conta bloqueada |
| 429 | Too Many Requests | Rate limit excedido |
| 500 | Internal Server Error | Erro interno do servidor |
| 503 | Service Unavailable | Serviço indisponível |

---

## Formato de Respostas de Erro

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensagem amigável do erro",
  "details": [
    {
      "field": "email",
      "message": "Email já está em uso"
    }
  ],
  "path": "/api/v1/users",
  "traceId": "abc123"
}
```

---

## Paginação

### Request
```
GET /api/v1/products?page=0&size=20&sort=price,desc
```

### Response
```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

| Parâmetro | Default | Máximo | Descrição |
|-----------|---------|--------|-----------|
| page | 0 | - | Número da página (0-indexed) |
| size | 20 | 100 | Itens por página |
| sort | - | - | Campo e direção (campo,asc ou campo,desc) |

---

> **Última atualização:** Janeiro 2026
> 
> Este documento deve ser atualizado sempre que houver mudanças nas regras de negócio.


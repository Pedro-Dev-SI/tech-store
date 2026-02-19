# 🧾 Order Service - TechStore

> Microserviço responsável por pedidos e pelo fluxo de compra, integrado ao Kafka e ao Inventory Service.

---

## ✅ Objetivo
- Criar e gerenciar pedidos
- Orquestrar o fluxo de compra com eventos
- Integrar com Inventory (reserva/liberação/baixa)
- Reagir a eventos Kafka

---

## 🧩 Regras de negócio (resumo prático)

### Entidades (com snapshot)
**Order**
- `id`
- `orderNumber` (único, ex: `TS-YYYYMMDD-XXXXX`)
- `userId`
- `status`
- `totalAmount`
- `shippingAddress` (JSON snapshot)
- `notes`
- `createdAt`, `updatedAt`

**OrderItem**
- `id`
- `orderId`
- `productId`
- `productName` (snapshot)
- `productSku` (snapshot)
- `quantity`
- `unitPrice` (snapshot)
- `totalPrice` (quantity * unitPrice)

**OrderStatusHistory**
- `id`
- `orderId`
- `fromStatus`
- `toStatus`
- `notes`
- `createdAt`
- `createdBy`

---

## ✅ Regras de criação do pedido
- Deve ter **pelo menos 1 item**
- `quantity >= 1` para cada item
- Usuário deve ter **endereço válido** (snapshot do endereço no pedido)
- Produto deve existir e estar **ativo**
- Preço vem **do product-service**, nunca do cliente
- Cria snapshot de **nome, sku e preço**
- Calcula `totalAmount` como soma dos itens
- Cria `OrderStatusHistory` inicial

---

## ✅ Regras de cancelamento
- Usuário: só pode cancelar se status = `PENDING_PAYMENT`
- Admin: pode cancelar `PENDING_PAYMENT`, `PAYMENT_CONFIRMED`, `PROCESSING`
- Cancelar dispara liberação de estoque
- Se já pago: inicia fluxo de reembolso

---

## ✅ Status do pedido (resumo)
```
PENDING_PAYMENT -> PAYMENT_CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED
PENDING_PAYMENT -> PAYMENT_FAILED -> CANCELLED
PENDING_PAYMENT -> CANCELLED
PAYMENT_CONFIRMED -> CANCELLED / REFUNDED
PROCESSING -> CANCELLED / REFUNDED
```

---

## ✅ Endpoints principais
| Endpoint | Método | Descrição | Auth |
|----------|--------|-----------|------|
| `/api/v1/orders` | POST | Criar pedido | USER |
| `/api/v1/orders` | GET | Meus pedidos | USER |
| `/api/v1/orders/{id}` | GET | Detalhes do pedido | USER (próprio) / ADMIN |
| `/api/v1/orders/{id}/cancel` | POST | Cancelar pedido | USER (próprio) / ADMIN |
| `/api/v1/orders/admin` | GET | Todos os pedidos | ADMIN |
| `/api/v1/orders/{id}/status` | PUT | Atualizar status | ADMIN |

---

## ✅ DTOs sugeridos (para você criar)

### CreateOrderRequest
- `items`: lista de `{ productId, quantity }`
- `addressId`
- `notes` (opcional)

### OrderResponse
- `id`, `orderNumber`, `userId`, `status`, `totalAmount`
- `shippingAddress` (snapshot)
- `items[]` (snapshot)
- `createdAt`, `updatedAt`

### OrderItemResponse
- `productId`, `productName`, `productSku`
- `quantity`, `unitPrice`, `totalPrice`

---

---

## 🧭 Fluxo principal (visão geral)

1) Cliente cria pedido (`PENDING`)
2) Order publica evento para reservar estoque
3) Inventory reserva e publica `inventory.stock.reserved`
4) Order consome o evento e muda para `RESERVED`
5) Pagamento aprovado (futuro)
6) Order publica confirmação de saída
7) Inventory confirma e publica `inventory.stock.confirmed`
8) Order consome e muda para `CONFIRMED`

---

## 🔌 Endpoints (planejados)

| Endpoint | Método | Descrição | Auth |
|----------|--------|-----------|------|
| `/api/v1/orders` | POST | Criar pedido | USER |
| `/api/v1/orders/{id}` | GET | Detalhes do pedido | USER/ADMIN |
| `/api/v1/orders/{id}/cancel` | POST | Cancelar pedido | USER/ADMIN |
| `/api/v1/orders` | GET | Listar pedidos | ADMIN |

---

## 📣 Kafka (o que vai existir)

### Tópicos consumidos (vindo do Inventory)
- `inventory.stock.reserved`
- `inventory.stock.released`
- `inventory.stock.confirmed`
- `inventory.stock.low-alert` (opcional para alertas/monitoramento)

### Tópicos produzidos (iniciados pelo Order)
- `order.stock.reserve` *(pedido criado)*
- `order.stock.release` *(pedido cancelado)*
- `order.stock.confirm` *(pagamento aprovado)*

---

## ⚙️ Configuração Kafka (application.yml)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: order-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

**Por quê isso importa?**
- `group-id`: garante que o consumer retome de onde parou.
- `auto-offset-reset=earliest`: facilita testes locais.
- `JsonSerializer/JsonDeserializer`: eventos em JSON, simples de depurar.

---

## 🧠 Como o Kafka vai funcionar aqui (explicado)

### 1) Producer
O Order publica eventos quando alguma ação acontece:
- Pedido criado → emite `order.stock.reserve`
- Pedido cancelado → emite `order.stock.release`
- Pagamento aprovado → emite `order.stock.confirm`

> Isso evita chamada síncrona direta ao Inventory.

### 2) Consumer
O Order escuta eventos do Inventory:
- `inventory.stock.reserved` → marca pedido como `RESERVED`
- `inventory.stock.released` → marca pedido como `CANCELLED`
- `inventory.stock.confirmed` → marca pedido como `CONFIRMED`

> Se o Order cair, o Kafka mantém o evento. Quando voltar, continua do último offset.

---

## 📌 Boas práticas que vamos seguir

- **Idempotência** nos consumers: o mesmo evento não pode quebrar o fluxo.
- **Outbox pattern** (futuro): garantir que eventos não se percam.
- **Versionamento** de eventos: usar `eventId` e `occurredAt`.
- **Logs claros** no consumer: saber quando o fluxo parou.

---

## 🧭 Roadmap (passo a passo detalhado)

1) Criar entidades básicas (`Order`, `OrderItem`, `OrderStatus`)
2) Criar DTOs de request/response
3) Criar repository, service e controller
4) Criar producer de eventos (`order.stock.reserve`, etc)
5) Criar consumer de eventos do inventory
6) Implementar transições de status no service
7) Testes unitários e de integração
8) Documentar endpoints no Swagger

---

## ⚙️ Stack
- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- Kafka (Spring Kafka)
- Validation
- Swagger (springdoc)

---

> Atualize esse README conforme o serviço evoluir.

# 📦 Inventory Service - TechStore

> Microserviço responsável pelo controle de estoque e movimentações.

---

## ✅ Objetivo
- Controlar estoque por produto
- Reservar e liberar estoque
- Registrar movimentações (auditoria)
- Publicar eventos de estoque via Kafka

---

## 🧩 Entidades

### Inventory
- `id` (UUID)
- `productId` (UUID, único)
- `quantity` (int)
- `reservedQuantity` (int)
- `minStockAlert` (int)
- `updatedAt` (DateTime)

### StockMovement
- `id` (UUID)
- `inventoryId` (UUID)
- `type` (IN/OUT/RESERVE/RELEASE)
- `quantity` (int)
- `reason` (String)
- `orderId` (UUID)
- `createdAt` (DateTime)

---

## 🔌 Endpoints (planejados)

| Endpoint | Método | Descrição | Auth |
|----------|--------|-----------|------|
| `/api/v1/inventory/{productId}` | GET | Consultar estoque | Interno/ADMIN |
| `/api/v1/inventory/{productId}` | PUT | Atualizar quantidade | ADMIN |
| `/api/v1/inventory/reserve` | POST | Reservar estoque | Interno |
| `/api/v1/inventory/release` | POST | Liberar reserva | Interno |
| `/api/v1/inventory/confirm` | POST | Confirmar baixa | Interno |
| `/api/v1/inventory/internal/{productId}` | GET | Consultar estoque (interno) | Interno |
| `/api/v1/inventory/low-stock` | GET | Produtos com estoque baixo | ADMIN |

---

## 📣 Eventos Kafka

### Tópicos publicados
- `inventory.stock.reserved`
- `inventory.stock.released`
- `inventory.stock.confirmed`
- `inventory.stock.low-alert`

### Payloads
Eventos publicados como JSON.

Exemplos:

**StockReservedEvent**
- `eventId` (UUID)
- `occurredAt` (Instant)
- `orderId` (UUID)
- `productId` (UUID)
- `quantity` (int)

**StockReleasedEvent**
- `eventId` (UUID)
- `occurredAt` (Instant)
- `orderId` (UUID)
- `productId` (UUID)
- `quantity` (int)

**StockConfirmedEvent**
- `eventId` (UUID)
- `occurredAt` (Instant)
- `orderId` (UUID)
- `productId` (UUID)
- `quantity` (int)

**StockLowAlertEvent**
- `eventId` (UUID)
- `occurredAt` (Instant)
- `productId` (UUID)
- `availableQuantity` (int)
- `minimumQuantity` (int)

### Quando publica
- Reserva: ao reservar estoque (`/reserve`)
- Liberação: ao liberar reserva (`/release`)
- Confirmação: ao confirmar saída (`/confirm`)
- Alerta: quando `quantity <= minStockAlert` após update ou confirmação

---

## 🧭 Roadmap (passo a passo)

1) Configurar banco e Flyway
2) Criar entidades Inventory e StockMovement
3) Criar repositories
4) DTOs de request/response
5) Services com regras de negócio
6) Controllers
7) Eventos Kafka (stock.reserved, stock.released, stock.confirmed, stock.low-alert)
8) Testes unitários

---

## ⚙️ Stack
- Java 21
- Spring Boot 3.2+
- Spring Data JPA
- PostgreSQL
- Flyway
- Kafka
- Validation

---

> Atualize este README conforme o serviço evoluir.

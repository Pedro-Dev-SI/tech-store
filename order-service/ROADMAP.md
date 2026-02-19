# 🧭 Order Service - Roadmap (Passo a passo)

Este roteiro é para você implementar o serviço **de forma incremental**, validando o fluxo com Kafka desde o começo.

---

## 1) Base do projeto
- Verifique o `application.yaml` (porta, db, kafka, swagger)
- Confirme dependências no `pom.xml`:
  - Spring Web
  - Spring Data JPA
  - Validation
  - PostgreSQL
  - Flyway
  - Spring Kafka
  - Springdoc OpenAPI

---

## 2) Modelagem mínima (core)
Crie as entidades:
- `Order`
  - `id`
  - `userId`
  - `status` (enum)
  - `totalAmount`
  - `createdAt`, `updatedAt`
- `OrderItem`
  - `id`
  - `orderId`
  - `productId`
  - `quantity`
  - `unitPrice`

Enum:
- `OrderStatus` → `PENDING`, `RESERVED`, `CONFIRMED`, `CANCELLED`

---

## 3) Migrations (Flyway)
Crie o schema inicial:
1. tabela `orders`
2. tabela `order_items`
3. índices por `user_id` e `status`

---

## 4) DTOs
Requests:
- `CreateOrderRequest`
  - `items[]` (productId, quantity)
- `CancelOrderRequest` (se quiser motivo)

Responses:
- `OrderResponse`
- `OrderItemResponse`

---

## 5) Repository
Crie:
- `OrderRepository`
- `OrderItemRepository`

Métodos úteis:
- `findByUserId`
- `findByStatus`

---

## 6) Service (regras de negócio)
Regras mínimas:
- `createOrder` cria pedido `PENDING`
- `cancelOrder` só se status permitir
- `confirmOrder` só após evento `stock.confirmed`

---

## 7) Producer (Order → Inventory)
Crie `OrderEventProducer` e publique:
- `order.stock.reserve` ao criar pedido
- `order.stock.release` ao cancelar
- `order.stock.confirm` ao confirmar pagamento

> Use `KafkaTemplate<String, Object>` e eventos JSON.

---

## 8) Consumer (Inventory → Order)
Crie `InventoryEventConsumer` escutando:
- `inventory.stock.reserved`
  - muda status para `RESERVED`
- `inventory.stock.released`
  - muda status para `CANCELLED`
- `inventory.stock.confirmed`
  - muda status para `CONFIRMED`

**Importante:** fazer idempotência:
- Se o pedido já está no status final, ignore o evento.

---

## 9) Controller
Endpoints mínimos:
- `POST /api/v1/orders` (criar)
- `GET /api/v1/orders/{id}` (detalhar)
- `POST /api/v1/orders/{id}/cancel` (cancelar)
- `GET /api/v1/orders` (listar/admin)

---

## 10) Testes mínimos
Crie testes para:
- criação de pedido
- mudança de status via consumer
- cancelamento

---

## 11) Integração com Gateway
No gateway:
- liberar `/api/v1/orders` para USER
- headers `X-User-Id` e `X-User-Role`

---

## 12) Validação final
1. Sobe Kafka + Inventory + Order
2. Cria pedido no Order → dispara reserva
3. Inventory recebe e publica reservado
4. Order consome e muda status para RESERVED

---

Quando concluir esse roadmap, o fluxo base já estará funcionando.

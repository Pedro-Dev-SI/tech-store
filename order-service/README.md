# Order Service - TechStore

> Microservice responsible for order lifecycle, business status transitions, and integrations with inventory and Kafka.

---

## Goal
- Create and manage customer orders
- Enforce order business rules and status transitions
- Integrate with inventory (reserve/release/confirm stock)
- Publish domain events for other services

---

## Business Rules

### Entities

**Order**
- `id`
- `orderNumber` (unique, format `TS-YYYYMMDD-XXXXX`)
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
- `totalPrice` (`quantity * unitPrice`)

**OrderStatusHistory**
- `id`
- `orderId`
- `fromStatus`
- `toStatus`
- `notes`
- `createdAt`
- `createdBy`

### Create Order Rules
- Order must contain at least one item
- Each item must have `quantity >= 1`
- User must provide a valid address
- Product must exist and be active
- Product price is always loaded from product-service
- Product and address data are saved as snapshots
- `totalAmount` is the sum of item totals
- Initial `OrderStatusHistory` entry is created

### Cancel Rules
- USER can cancel only in `PENDING_PAYMENT`
- ADMIN can cancel in `PENDING_PAYMENT`, `PAYMENT_CONFIRMED`, `PROCESSING`
- Cancellation may trigger stock release (based on current status)

### Status Flow
```
PENDING_PAYMENT -> PAYMENT_CONFIRMED -> PROCESSING -> SHIPPED -> DELIVERED
PENDING_PAYMENT -> PAYMENT_FAILED
PENDING_PAYMENT -> CANCELLED
PAYMENT_CONFIRMED -> CANCELLED / REFUNDED
PROCESSING -> CANCELLED / REFUNDED
```

---

## Endpoints

| Endpoint | Method | Description | Auth |
|----------|--------|-------------|------|
| `/api/v1/orders` | POST | Create order | USER |
| `/api/v1/orders` | GET | List my orders | USER |
| `/api/v1/orders/{id}` | GET | Get order details | OWNER / ADMIN |
| `/api/v1/orders/{id}/cancel` | POST | Cancel order | OWNER / ADMIN |
| `/api/v1/orders/admin` | GET | List all orders | ADMIN |
| `/api/v1/orders/{id}/status` | PUT | Update order status | ADMIN |

---

## Kafka

### Produced topics
- `order.created`
- `order.cancelled`
- `order.paid`
- `order.shipped`

### Expected consumed topics (future/optional)
- `inventory.stock.reserved`
- `inventory.stock.released`
- `inventory.stock.confirmed`
- `inventory.stock.low-alert`

---

## DTOs

### Request DTOs
- `CreateOrderRequest` (`items[]`, `addressId`, `notes`)
- `CancelOrderRequest` (`reason`)
- `UpdateOrderStatusRequest` (`status`, `notes`)

### Response DTOs
- `OrderResponse`
- `OrderItemResponse`

---

## Integration Points

- **user-service**
  - validate current user
  - validate and fetch selected address
- **product-service**
  - fetch products by IDs (active only)
  - read authoritative product price/name/sku
- **inventory-service**
  - reserve stock on order creation
  - release/confirm stock depending on status transitions

---

## Stack
- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Kafka
- OpenFeign
- MapStruct
- Swagger (springdoc)

---

> Keep this README synchronized with implemented business rules and integration behavior.

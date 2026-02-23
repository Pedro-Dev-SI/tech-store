# Inventory Service - TechStore

> Microservice responsible for stock control and stock movements.

---

## Goal
- Control stock per product
- Reserve and release stock
- Track stock movements (audit trail)
- Publish stock events to Kafka

---

## Entities

### Inventory
- `id` (UUID)
- `productId` (UUID, unique)
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

## Endpoints

| Endpoint | Method | Description | Auth |
|----------|--------|-------------|------|
| `/api/v1/inventory/{productId}` | GET | Get stock by product | Internal/ADMIN |
| `/api/v1/inventory/{productId}` | PUT | Update stock quantity | ADMIN |
| `/api/v1/inventory/reserve` | POST | Reserve stock | Internal |
| `/api/v1/inventory/release` | POST | Release reservation | Internal |
| `/api/v1/inventory/confirm` | POST | Confirm stock output | Internal |
| `/api/v1/inventory/internal/{productId}` | GET | Internal stock query | Internal |
| `/api/v1/inventory/low-stock` | GET | List low-stock products | ADMIN |

---

## Kafka Events

### Published topics
- `inventory.stock.reserved`
- `inventory.stock.released`
- `inventory.stock.confirmed`
- `inventory.stock.low-alert`

### Event payloads (JSON)

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

### Publish conditions
- Reserve event: after `/reserve`
- Release event: after `/release`
- Confirm event: after `/confirm`
- Low alert event: when `quantity <= minStockAlert` after update or confirmation

---

## Stack
- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- Kafka
- Bean Validation

---

> Keep this README updated as the service evolves.

# TechStore - Microservices Platform

TechStore is an e-commerce backend built with a microservices architecture.

## Services

- `api-gateway` - single entry point, JWT validation, routing
- `auth-service` - authentication and token lifecycle
- `user-service` - user profile and address management
- `product-service` - products, categories, catalog search
- `inventory-service` - stock management and stock events
- `order-service` - order lifecycle and business transitions

## Infrastructure

- PostgreSQL (multiple databases, one per service)
- Kafka (event-driven communication where needed)
- Docker Compose files for local development and containerized runs

## Default Local Ports

- Gateway: `8080`
- Auth: `8081`
- User: `8082`
- Product: `8083`
- Inventory: `8084`
- Order: `8085`
- Postgres (dev compose): `5433`

## Collections

Postman files are available in `collections/`:
- `techstore-full.postman_collection.json`
- `techstore-full.postman_environment.json`

## Run Notes

- Start infrastructure (`docker-compose.dev.yml`) for local databases and Kafka.
- Start each service locally (IDE or Maven wrapper).
- Use gateway endpoints (`http://localhost:8080`) for end-to-end API testing.

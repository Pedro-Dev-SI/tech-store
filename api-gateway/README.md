# API Gateway - TechStore

> Single entry point for all microservices. Centralizes authentication, routing, and shared policies.

---

## What the Gateway does (product view)

- **Routing**: receives all requests and forwards them to the correct service.
- **Authentication**: validates JWT once before allowing access.
- **Authorization**: blocks ADMIN/USER protected routes when needed.
- **CORS**: controls front-end access rules.
- **Observability**: centralized logs and metrics.
- **Rate limiting** (future): abuse protection.

---

## Current Routes

| Path | Service |
|------|---------|
| `/api/v1/auth/**` | auth-service |
| `/api/v1/users/**` | user-service |
| `/api/v1/products/**` | product-service |
| `/api/v1/categories/**` | product-service |
| `/api/v1/inventory/**` | inventory-service |
| `/api/v1/orders/**` | order-service |

---

## Build Steps

1) **Base configuration**
- Configure `application.yaml` with service URLs.
- Define gateway port (example: 8080).

2) **Routes**
- Create route definitions in `application.yaml`.
- Validate basic routing first.

3) **Auth Filter**
- Create a global filter that:
  - reads `Authorization` header
  - calls `/api/v1/auth/validate`
  - blocks invalid tokens
  - allows valid tokens

4) **Public vs protected routes**
- `/api/v1/auth/**` and public product/category GET routes are public.
- Protected routes require valid JWT.

5) **Claims propagation**
- Inject `X-User-Id` and `X-User-Role` for downstream services.

6) **Observability**
- Enable `/actuator/health`.
- Keep request logs enabled.

---

## Stack

- Spring Cloud Gateway (WebFlux)
- Spring Boot
- Actuator
- Validation

---

## Notes

- The gateway is the **single public entry point**.
- In production, internal services should not be directly exposed.
- Swagger can aggregate docs from all microservices.

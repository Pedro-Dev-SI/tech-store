# Auth Service - TechStore E-Commerce

> Microservice responsible for authentication, JWT issuance, and refresh-token lifecycle.

---

## Business Rules (PO view)

### Service Goal
Provide secure login, token issuance/renewal, and session control.

### Entity

#### RefreshToken
| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Auto-generated |
| `token` | String | Unique token |
| `userId` | UUID | User identifier |
| `expiryDate` | DateTime | Refresh token expiration |
| `revoked` | Boolean | Revoked on logout |
| `createdAt` | DateTime | Auto-generated |

---

## Detailed Rules

### Tokens
- Access token expires in **15 minutes**.
- Refresh token expires in **7 days**.
- Maximum **5 active refresh tokens** per user (revoke oldest first).

### Password and lock policy
- Password requires **minimum 8 chars**, **1 uppercase**, **1 number**.
- **5 failed login attempts** lock the user for **15 minutes**.

---

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/register` | POST | Register new user (calls user-service) |
| `/api/v1/auth/login` | POST | Authenticate and issue tokens |
| `/api/v1/auth/refresh` | POST | Issue new access token |
| `/api/v1/auth/logout` | POST | Revoke refresh token |
| `/api/v1/auth/validate` | GET | Validate token (used by gateway) |

---

## Build Flow

1) **Configuration**
- Configure `application.yaml` with `auth_db`, JWT settings, and user-service URL.

2) **Modeling**
- Create `RefreshToken` entity with unique token constraint.

3) **Repository**
- Query by token, list by `userId`, and remove old tokens.

4) **Service**
- `register`: call user-service and return tokens
- `login`: validate credentials, control failed attempts, issue tokens
- `refresh`: validate refresh token and issue new access token
- `logout`: revoke refresh token
- `validate`: validate JWT

5) **JWT**
- Implement `JwtService` for token generation/validation.
- Keep secret and expirations in configuration.

6) **Controller**
- Expose REST endpoints with request/response DTOs.

7) **Tests**
- Unit tests for token validation, expiration, and token limits.

---

## Technical Details

### Stack
- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Security (crypto)
- JWT

### Notes
- Depends on **user-service** for register/login user data.
- API Gateway uses `/api/v1/auth/validate` before forwarding protected requests.

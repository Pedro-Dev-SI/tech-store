# User Service - TechStore E-Commerce

> Microservice responsible for user registration, profile management, and addresses.

---

## Product View (Business Rules)

### Service Goal
Provide complete user and address management with strict data validation and clear security rules.

### Entities

#### User
| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Auto-generated |
| `email` | String | Unique, valid format |
| `password` | String | BCrypt hash only (never plain text) |
| `name` | String | Required, 2-100 chars |
| `cpf` | String | Unique, must pass CPF algorithm |
| `phone` | String | Valid phone format |
| `role` | Enum | USER or ADMIN (default USER) |
| `status` | Enum | ACTIVE, INACTIVE, BLOCKED |
| `createdAt` | DateTime | Auto-generated |
| `updatedAt` | DateTime | Auto-updated |

#### Address
| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Auto-generated |
| `userId` | UUID | FK to User |
| `street` | String | Required |
| `number` | String | Required |
| `complement` | String | Optional |
| `neighborhood` | String | Required |
| `city` | String | Required |
| `state` | String | 2-char state code |
| `zipCode` | String | `XXXXX-XXX` format |
| `isDefault` | Boolean | Exactly one default address when user has addresses |

---

## Detailed Rules

### User
- Email and CPF must be unique.
- CPF must be algorithmically valid.
- Password is never stored in plain text (BCrypt).
- Soft delete: user becomes `INACTIVE`.
- `INACTIVE` or `BLOCKED` users cannot authenticate.

### Address
- Maximum **5 addresses** per user.
- Must always keep **one default address** if there is at least one address.
- First created address becomes default automatically.
- Setting a new default unsets old default.
- If default is removed, oldest remaining address becomes default.

---

## Endpoints

### Users
| Endpoint | Method | Description | Auth |
|----------|--------|-------------|------|
| `/api/v1/users` | POST | Create user | Public |
| `/api/v1/users/me` | GET | Get current profile | USER |
| `/api/v1/users/me` | PATCH | Update current profile | USER |
| `/api/v1/users/{id}` | GET | Get user by id | ADMIN |
| `/api/v1/users` | GET | List users | ADMIN |
| `/api/v1/users/{id}` | DELETE | Deactivate user | ADMIN |

### Addresses
| Endpoint | Method | Description | Auth |
|----------|--------|-------------|------|
| `/api/v1/users/me/addresses` | GET | List addresses | USER |
| `/api/v1/users/me/addresses/default` | GET | Get default address | USER |
| `/api/v1/users/me/addresses/{id}` | GET | Get address by id | USER |
| `/api/v1/users/me/addresses` | POST | Add address | USER |
| `/api/v1/users/me/addresses/{id}` | PATCH | Update address | USER |
| `/api/v1/users/me/addresses/{id}` | DELETE | Delete address | USER |

---

## Main Flows

### Create User
1. Validate email, CPF, and password format.
2. Check email/CPF uniqueness.
3. Hash password using BCrypt.
4. Save user with `role=USER` and `status=ACTIVE`.

### Update User
1. Require authenticated user.
2. Do not allow public profile endpoint to change `role` or `status`.
3. Validate changed fields.
4. Save changes.

### Add Address
1. Validate max 5 addresses limit.
2. Validate address fields.
3. First address becomes default automatically.
4. If `isDefault=true`, unset previous default.

---

## Technical Details

### Stack
- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway
- Bean Validation

### Package Structure (Layered)
```
com.br.userservice/
├── controller/
├── service/
├── repository/
├── model/
├── dto/
├── mapper/
├── exception/
└── config/
```

### Conventions
- Controllers expose DTOs only (no entities).
- Business rules stay in service layer.
- Repositories focus on persistence.
- Error payloads follow project standard format.

---

## Notes
- This service is a core dependency of `auth-service`.
- Gateway validates JWT and injects identity headers for protected calls.

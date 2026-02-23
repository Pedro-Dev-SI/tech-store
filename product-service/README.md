# Product Service - TechStore E-Commerce

> Microservice responsible for product and category catalog management.

---

## Overview

The **Product Service** handles:
- Product catalog management (CRUD + soft delete)
- Category management with hierarchy support (up to 3 levels)
- Product images and attributes
- Search and listing endpoints with filters
- Catalog data integrity and consistency

### Technologies
- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL
- Flyway

---

## Entities

### Category
| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Auto-generated |
| `name` | String | Required, unique within same parent level |
| `slug` | String | Generated from name, globally unique |
| `description` | String | Optional |
| `parentId` | UUID | FK to Category |
| `active` | Boolean | Default `true` |
| `createdAt` | DateTime | Auto-generated |

Key points:
- Hierarchy max depth: **3 levels**
- Global slug uniqueness
- Inactive categories are hidden from public listing

### Product
| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Auto-generated |
| `sku` | String | Required, unique |
| `name` | String | Required, 3-200 chars |
| `slug` | String | Generated from name, unique |
| `description` | Text | Optional |
| `brand` | String | Required |
| `categoryId` | UUID | Required FK to active Category |
| `price` | Decimal | Required, min `0.01` |
| `compareAtPrice` | Decimal | Optional, if present must be greater than `price` |
| `active` | Boolean | Default `true` |
| `createdAt` | DateTime | Auto-generated |
| `updatedAt` | DateTime | Auto-updated |

Key points:
- SKU must be unique
- Slug auto-generated
- Soft delete (`active = false`)

### ProductImage
| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Auto-generated |
| `productId` | UUID | FK to Product |
| `url` | String | Required URL |
| `altText` | String | Optional |
| `position` | Integer | Display order |
| `isMain` | Boolean | Main image flag |

Rules:
- Max **10 images** per product
- Exactly one main image

### ProductAttribute
| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Auto-generated |
| `productId` | UUID | FK to Product |
| `name` | String | Required |
| `value` | String | Required |

---

## Endpoints

### Products
| Endpoint | Method | Description | Auth |
|----------|--------|-------------|------|
| `/api/v1/products` | GET | List products (filters + pagination) | Public |
| `/api/v1/products/{id}` | GET | Product details | Public |
| `/api/v1/products/slug/{slug}` | GET | Get by slug | Public |
| `/api/v1/products/search` | GET | Text search | Public |
| `/api/v1/products/list/all` | POST | Get products by IDs | Internal |
| `/api/v1/products` | POST | Create product | ADMIN |
| `/api/v1/products/{id}` | PATCH | Partial update | ADMIN |
| `/api/v1/products/{id}` | DELETE | Inactivate product | ADMIN |
| `/api/v1/products/{id}/images` | POST | Add images | ADMIN |
| `/api/v1/products/{id}/images/{imageId}` | DELETE | Remove image | ADMIN |
| `/api/v1/products/{id}/attributes` | POST | Add attributes | ADMIN |

### Categories
| Endpoint | Method | Description | Auth |
|----------|--------|-------------|------|
| `/api/v1/categories` | GET | List categories | Public |
| `/api/v1/categories/{id}` | GET | Category details | Public |
| `/api/v1/categories` | POST | Create category | ADMIN |
| `/api/v1/categories/{id}` | PATCH | Partial update | ADMIN |
| `/api/v1/categories/{id}` | DELETE | Deactivate category | ADMIN |

---

## Business Rules

### Product Rules
- Unique SKU
- Unique slug (numeric suffix when duplicated)
- Slug generation: normalize, lowercase, hyphen-separated
- Price minimum `0.01`
- `compareAtPrice > price` when provided
- Product must reference an active category
- Soft delete only (`active = false`)

Slug example:
```
iPhone 15 Pro Max 256GB -> iphone-15-pro-max-256gb
duplicate -> iphone-15-pro-max-256gb-2
```

### Category Rules
- Max hierarchy depth: 3
- Global unique slug
- Unique name per parent level (including inactive entries)
- Category with products cannot be deactivated
- Parent category deactivation must respect children business constraints

### Image Rules
- Max 10 per product
- Exactly one main image
- Main image replacement keeps consistency

---

## Filters

### `GET /api/v1/products`
Supported query params:
- `categoryId`, `minPrice`, `maxPrice`, `brand`, `search`, `active`
- `sortBy` (`price`, `name`, `createdAt`, `relevance`)
- `sortDirection` (`asc`, `desc`)
- `page`, `size` (max size: 100)

### `GET /api/v1/products/search`
- `q` required
- `page`, `size`, `sortBy`, `sortDirection`

---

## Notes

1. Product and category deletions are soft-delete style.
2. Category hierarchy supports up to 3 levels.
3. Slugs are auto-generated and unique.
4. Inactive products/categories are hidden from public endpoints.
5. Product creation should initialize inventory with quantity `0`.

---

> Last update: January 2026

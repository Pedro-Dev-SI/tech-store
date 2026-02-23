# Collections

## Files
- `techstore-full.postman_collection.json`
- `techstore-full.postman_environment.json`

## What is covered
- Endpoints for:
  - `auth-service`
  - `user-service`
  - `product-service` (products + categories)
  - `inventory-service`
  - `order-service`
- Automatic scripts to save:
  - `accessToken`, `refreshToken`
  - `userId`, `userRole`
  - `addressId`, `categoryId`, `productId`, `productSlug`, `orderId`
- Updated environment variables for all current flows.

## How to use
1. Import `techstore-full.postman_collection.json`.
2. Import `techstore-full.postman_environment.json`.
3. Select the environment in Postman.
4. Start the services:
   - `api-gateway` (port `8080`)
   - `auth-service` (port `8081`)
   - `user-service` (port `8082`)
   - `product-service` (port `8083`)
   - `inventory-service` (port `8084`)
   - `order-service` (port `8085`)

## Important notes
- Order endpoints go through gateway (`{{gatewayUrl}}`).
- Admin endpoints require a token with `ADMIN` role.
- Inventory internal calls are routed by gateway with internal headers.
- `Delete Image (Admin)` uses `{{imageId}}`; set it manually.
- In `user-service`, create-address DTO currently expects `ziCode`; collection already sends this field.

## Suggested happy path
1. `Auth > Register` (or `Auth > Login`)
2. `Users > Me`
3. `Addresses > Add Address`
4. `Categories > Create Category (Admin)`
5. `Products > Create Product (Admin)`
6. `Orders > Create Order`
7. `Orders > List My Orders`

# Collections

## Arquivos
- initial-test-auth.postman_collection.json
- initial-test-auth.postman_environment.json
- techstore-full.postman_collection.json
- techstore-full.postman_environment.json

## Como usar
1) Importe a collection no Postman.
2) Importe o environment correspondente.
3) Selecione o environment no Postman.
4) Suba os serviços: auth-service, user-service, product-service e api-gateway.

## Fluxo sugerido
### Initial Test Auth
1) Auth Register
2) Auth Login
3) Public Products (GET)
4) Protected Products (POST) - No Token
5) Protected Products (POST) - With Token

### TechStore Full Flow
1) Auth > Register
2) Auth > Login
3) Users > Me
4) Users > Update Me (PATCH)
5) Addresses > Add Address
6) Addresses > Update Address (set addressId manually if needed)
7) Addresses > Delete Address
8) Products > List Products (Public)
9) Products > Create Product (Protected)

## Observacoes
- Os requests de register/login salvam accessToken e refreshToken automaticamente.
- Ajuste o gatewayUrl se usar outra porta.
- Os POSTs de produto usam categoryId dummy; ajuste se quiser criar um produto real.
- Para o fluxo completo, preencha categoryId e addressId quando necessario.

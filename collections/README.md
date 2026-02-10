# Collections - Initial Test Auth

## Arquivos
- initial-test-auth.postman_collection.json
- initial-test-auth.postman_environment.json

## Como usar
1) Importe a collection no Postman.
2) Importe o environment "Initial Test Auth (Local)".
3) Selecione o environment no Postman.
4) Suba os serviços: auth-service, user-service, product-service e api-gateway.

## Fluxo sugerido
1) Auth Register
2) Auth Login
3) Public Products (GET)
4) Protected Products (POST) - No Token
5) Protected Products (POST) - With Token

## Observacoes
- Os requests de register/login salvam accessToken e refreshToken automaticamente.
- Ajuste o gatewayUrl se usar outra porta.
- Os POSTs de produto usam categoryId dummy; ajuste se quiser criar um produto real.

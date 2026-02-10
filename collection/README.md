# Collection - Product Service

## Arquivos
- product-service.postman_collection.json
- product-service.postman_environment.json

## Como usar
1) Importe a collection no Postman.
2) Importe o environment "TechStore - Product Service (Local)".
3) Selecione o environment no Postman.
4) Suba o product-service em http://localhost:8083.

## Variaveis do environment
- baseUrl: URL base do service (default: http://localhost:8083)
- categoryId: preenchido automaticamente apos criar categoria
- parentCategoryId: preenchido automaticamente apos criar categoria raiz
- productId: preenchido automaticamente apos criar produto
- imageId: preenchido automaticamente apos adicionar imagem

## Fluxo Happy Path (recomendado)
Execute a pasta "Happy Path" na ordem:
1. Create Category (Root)
2. Create Category (Child)
3. Update Category (PATCH)
4. Create Product
5. Add Product Images
6. Add Product Attributes
7. Update Product (PATCH)
8. Get Product By Id

## Observacoes
- Os requests de criacao configuram as variaveis automaticamente via testes.
- Se algum id estiver vazio, confira a resposta do request anterior.
- Ajuste o baseUrl se estiver usando outra porta.

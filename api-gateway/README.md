# 🌐 API Gateway - TechStore

> Ponto de entrada único para todos os microserviços. Centraliza autenticação, roteamento e políticas comuns.

---

## ✅ O que o Gateway faz (visão de produto)

- **Roteamento**: recebe todas as requisições e direciona para o serviço correto.
- **Autenticação**: valida JWT uma única vez antes de liberar o acesso.
- **Autorização**: bloqueia rotas de ADMIN/USER.
- **CORS**: configurações de acesso do front-end.
- **Observabilidade**: logs e métricas centralizadas.
- **Rate limit** (futuro): proteção contra abuso.

---

## 🔌 Rotas previstas

| Path | Serviço |
|------|---------|
| `/api/v1/auth/**` | auth-service |
| `/api/v1/users/**` | user-service |
| `/api/v1/products/**` | product-service |
| `/api/v1/categories/**` | product-service |

---

## 🧭 Passo a passo (construção)

1) **Configuração base**
- Ajustar `application.yaml` com as URLs dos serviços
- Definir porta do gateway (ex: 8080)
- Exemplo mínimo:
```yaml
server:
  port: 8080

services:
  auth: http://localhost:8081
  users: http://localhost:8082
  products: http://localhost:8083
```

2) **Rotas**
- Criar rotas no `application.yaml`
- Validar roteamento básico (sem auth)
 - Exemplo:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: ${services.auth}
          predicates:
            - Path=/api/v1/auth/**
        - id: user-service
          uri: ${services.users}
          predicates:
            - Path=/api/v1/users/**
        - id: product-service
          uri: ${services.products}
          predicates:
            - Path=/api/v1/products/**, /api/v1/categories/**
```

3) **Auth Filter**
- Criar filtro global que:
  - lê o header `Authorization`
  - chama `/api/v1/auth/validate`
  - bloqueia se inválido
  - libera se válido

4) **Rotas públicas x protegidas**
- Auth e produtos GET são públicos
- Usuários e produtos admin são protegidos

5) **Propagação de claims (futuro)**
- Passar `X-User-Id` e `X-User-Role` para serviços internos

6) **Observabilidade**
- Habilitar `/actuator/health`
- Logs de requests

---

## ⚙️ Stack

- Spring Cloud Gateway (WebFlux)
- Spring Boot 3.2+
- Actuator
- Validation

---

## 📌 Observações

- O gateway é o **único ponto de entrada** do sistema.
- Em produção, ele deve validar tokens antes de liberar acesso.
- Os serviços internos não devem expor endpoints diretamente ao cliente final.

---

> Este README evolui conforme a segurança e os filtros são implementados.

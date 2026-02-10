# 🔐 Auth Service - TechStore E-Commerce

> Microserviço responsável por autenticação, emissão de JWT e refresh tokens.

---

## 📌 Regras de Negócio (visão de P.O.)

### Objetivo do serviço
Garantir login seguro, emissão e renovação de tokens, e controle de sessões.

### Entidades

#### RefreshToken
| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | UUID | Gerado automaticamente |
| `token` | String | Único, 256 caracteres aleatórios |
| `userId` | UUID | ID do usuário |
| `expiryDate` | DateTime | Expiração do refresh |
| `revoked` | Boolean | Revogado no logout |
| `createdAt` | DateTime | Gerado automaticamente |

---

## ✅ Regras de Negócio (detalhadas)

### Tokens
- Access token expira em **15 minutos**
- Refresh token expira em **7 dias**
- **Máximo de 5 refresh tokens ativos** por usuário (revogar os mais antigos)

### Senha e bloqueio
- Senha com **mínimo 8 caracteres**, **1 maiúscula**, **1 número**
- **5 tentativas falhas** de login → bloqueia por **15 minutos**

---

## 🔌 Endpoints

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/api/v1/auth/register` | POST | Registrar novo usuário (chama user-service) |
| `/api/v1/auth/login` | POST | Autenticar e gerar tokens |
| `/api/v1/auth/refresh` | POST | Renovar access token |
| `/api/v1/auth/logout` | POST | Revogar refresh token |
| `/api/v1/auth/validate` | GET | Validar token (gateway) |

---

## 🧭 Passo a passo (roteiro de construção)

1) **Configuração**
- `application.yaml` com DB `auth_db`, JWT config e URL do user-service

2) **Modelagem**
- Entidade `RefreshToken`
- Campos + constraints (token único)

3) **Repository**
- `RefreshTokenRepository`
- buscar por token, listar por userId, deletar tokens antigos

4) **Service**
- `register`: chama user-service e retorna tokens
- `login`: valida credenciais, controla tentativas, gera tokens
- `refresh`: valida refresh token e emite novo access
- `logout`: revoga refresh token
- `validate`: valida JWT

5) **JWT**
- Criação de `JwtService` (gerar/validar tokens)
- Secret e expiração via config

6) **Controller**
- Endpoints REST
- DTOs de request/response

7) **Testes**
- Unitários: validações de tokens, expiração, max tokens

---

## ⚙️ Detalhamento Técnico

### Stack
- Java 21
- Spring Boot 3.2+
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Security (crypto)
- JWT (biblioteca a definir)

### Observações
- Este serviço depende do **user-service** para login e register.
- O API Gateway chamará `/auth/validate` para validar o JWT.

---

> Este README deve evoluir junto com o serviço.

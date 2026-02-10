# ROADMAP - User Service (TechStore)

> Guia passo a passo para construir o user-service seguindo as regras do projeto.

---

## FASE 1 — Fundacao do Servico
1. Revisar README e regras gerais do projeto.
2. Definir stack final (Java 21, Spring Boot, JPA, PostgreSQL, Flyway).
3. Configurar `application.yaml` (porta, banco, Flyway, JPA).
4. Preparar estrutura de pacotes (controller, service, repository, model, dto, mapper, exception).

---

## FASE 2 — Modelagem de Domínio
1. Criar entidade `User` com campos e restricoes.
2. Criar entidade `Address` com relacao para `User`.
3. Definir enums: `Role`, `UserStatus`.
4. Configurar constraints JPA (unique para email e cpf).

---

## FASE 3 — Repositorios
1. Criar `UserRepository`.
2. Criar `AddressRepository`.
3. Adicionar metodos:
   - `existsByEmail`, `existsByCpf`
   - `findByEmail`
   - `findByUserId`

---

## FASE 4 — DTOs e Mapper
1. DTOs de request:
   - `CreateUserRequest`
   - `UpdateUserRequest`
   - `CreateAddressRequest`
   - `UpdateAddressRequest`
2. DTOs de response:
   - `UserResponse`
   - `AddressResponse`
3. Criar `UserMapper` e `AddressMapper` (MapStruct ou manual).

---

## FASE 5 — Regras de Negocio (Service)
### Usuarios
1. Validar email unico.
2. Validar CPF unico e valido.
3. Hash da senha com BCrypt.
4. Status default = ACTIVE, role default = USER.
5. Soft delete: apenas marcar `status = INACTIVE`.

### Enderecos
1. Limite de 5 enderecos.
2. Garantir 1 endereco padrao (isDefault).
3. Primeiro endereco vira padrao automatico.
4. Alterar padrao quando necessario.

---

## FASE 6 — Controllers (API)
Implementar endpoints conforme README:
- POST `/api/v1/users`
- GET `/api/v1/users/me`
- PUT `/api/v1/users/me`
- GET `/api/v1/users/{id}`
- GET `/api/v1/users`
- DELETE `/api/v1/users/{id}`
- GET `/api/v1/users/me/addresses`
- POST `/api/v1/users/me/addresses`
- PUT `/api/v1/users/me/addresses/{id}`
- DELETE `/api/v1/users/me/addresses/{id}`
- GET `/api/v1/users/email/{email}` (interno)

---

## FASE 7 — Tratamento de Erros
1. Criar excecoes customizadas.
2. Criar `GlobalExceptionHandler`.
3. Padronizar resposta de erro conforme TechStore.

---

## FASE 8 — Testes
1. Testes unitarios de UserService e AddressService.
2. Casos criticos: CPF invalido, email duplicado, limite de enderecos.

---

## FASE 9 — Documentacao
1. Atualizar README com exemplos de requests.
2. Documentar erros comuns.

---

## FASE 10 — Revisao Final
1. Rodar testes.
2. Validar regras do README.
3. Revisar se endpoints batem com gateway futuro.

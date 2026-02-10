# 👤 User Service - TechStore E-Commerce

> Microserviço responsável por cadastro, perfil e endereços de usuários.

---

## 📌 Visão do P.O. (Regras de Negócio)

### Objetivo do serviço
Garantir o gerenciamento completo de usuários e endereços, com validações fortes de dados e regras claras de segurança.

### Entidades

#### User (Usuário)
| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | UUID | Gerado automaticamente |
| `email` | String | **Único**, formato válido |
| `password` | String | Hash BCrypt (nunca texto puro) |
| `name` | String | Obrigatório, 2-100 caracteres |
| `cpf` | String | **Único**, válido pelo algoritmo de CPF |
| `phone` | String | Formato: (XX) XXXXX-XXXX |
| `role` | Enum | USER ou ADMIN (default: USER) |
| `status` | Enum | ACTIVE, INACTIVE, BLOCKED |
| `createdAt` | DateTime | Gerado automaticamente |
| `updatedAt` | DateTime | Atualizado automaticamente |

#### Address (Endereço)
| Campo | Tipo | Regras |
|-------|------|--------|
| `id` | UUID | Gerado automaticamente |
| `userId` | UUID | FK para User |
| `street` | String | Obrigatório |
| `number` | String | Obrigatório |
| `complement` | String | Opcional |
| `neighborhood` | String | Obrigatório |
| `city` | String | Obrigatório |
| `state` | String | UF com 2 caracteres |
| `zipCode` | String | Formato XXXXX-XXX |
| `isDefault` | Boolean | Sempre deve existir exatamente 1 endereço padrão (se houver endereços) |

---

## ✅ Regras de Negócio (Detalhadas)

### Usuário
- **Email e CPF são únicos** no sistema.
- **CPF precisa ser válido** (algoritmo oficial).
- **Senha nunca é armazenada em texto puro** (usar BCrypt).
- **Soft delete**: usuário não é apagado, apenas fica `INACTIVE`.
- Usuário **INACTIVE** ou **BLOCKED** não pode autenticar.

### Endereços
- Máximo de **5 endereços por usuário**.
- Sempre deve existir **1 endereço padrão** (`isDefault = true`) quando existir pelo menos 1 endereço.
- Ao criar o **primeiro endereço**, ele vira padrão automaticamente.
- Se marcar um novo endereço como padrão, o anterior perde o status.
- Se o endereço padrão for removido, o mais antigo restante vira padrão.

---

## 🔌 Endpoints (Regras do Cliente)

### Usuários
| Endpoint | Método | Descrição | Auth |
|----------|--------|-----------|------|
| `/api/v1/users` | POST | Criar usuário | Público |
| `/api/v1/users/me` | GET | Meu perfil | USER |
| `/api/v1/users/me` | PUT | Atualizar meu perfil | USER |
| `/api/v1/users/{id}` | GET | Buscar por ID | ADMIN |
| `/api/v1/users` | GET | Listar todos | ADMIN |
| `/api/v1/users/{id}` | DELETE | Desativar usuário | ADMIN |
| `/api/v1/users/email/{email}` | GET | Buscar por email (interno) | Interno |

### Endereços
| Endpoint | Método | Descrição | Auth |
|----------|--------|-----------|------|
| `/api/v1/users/me/addresses` | GET | Listar endereços | USER |
| `/api/v1/users/me/addresses` | POST | Adicionar endereço | USER |
| `/api/v1/users/me/addresses/{id}` | PUT | Atualizar endereço | USER |
| `/api/v1/users/me/addresses/{id}` | DELETE | Remover endereço | USER |

---

## 🧠 Fluxos (PO descrevendo comportamento esperado)

### Criar Usuário
1) Validar email, CPF e senha  
2) Verificar unicidade de email e CPF  
3) Hash da senha com BCrypt  
4) Salvar usuário com `role=USER` e `status=ACTIVE`  
5) Publicar evento `user.registered` (futuro Kafka)

### Atualizar Usuário
1) Usuário autenticado  
2) Não permitir alterar `role` e `status` via endpoint público  
3) Validar campos alterados  
4) Salvar alterações

### Adicionar Endereço
1) Verificar limite de 5 endereços  
2) Validar dados do endereço  
3) Se for o primeiro endereço, vira padrão automaticamente  
4) Se `isDefault=true`, remover padrão dos demais  

---

## ⚙️ Detalhamento Técnico (Baseado no TechStore)

### Stack
- Java 21
- Spring Boot 3.2+
- Spring Data JPA
- PostgreSQL
- Flyway
- Validações com Bean Validation

### Estrutura de Pacotes (Package by Layer)
```
com.br.userservice/
├── controller/
├── service/
├── repository/
├── model/
├── dto/
│   ├── request/
│   └── response/
├── mapper/
├── exception/
└── config/
```

### Padrões e Convenções
- Controllers expõem apenas DTOs (nunca entidades).
- Regras de negócio ficam no Service.
- Repositórios apenas acesso ao banco.
- Erros retornam no formato padrão do projeto (timestamp, status, message, details).

---

## 📌 Observações Importantes
- Este serviço é **base para o auth-service**.
- O auth-service irá consultar o user-service para login e cadastro.
- API Gateway entrará após auth-service, para validar JWT e proteger rotas.

---

> Este README deve ser atualizado conforme o serviço evoluir.

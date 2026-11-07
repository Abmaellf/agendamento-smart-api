# Authentication and Users Specification

## Purpose

Fluxos observados em `AuthenticationController`, `UserController`, `SecurityFilter`, `SecurityConfiguration`, `TokenService` e `UserService`.

## Requirements

### Requirement: Login emite JWT e cookie

O sistema MUST aceitar `POST /auth/login` com login e senha, autenticar pelo Spring Security e devolver JWT no corpo e no cookie `jwt` HTTP-only. O cookie usa `SameSite=Lax`, caminho `/` e duração de uma hora; o token usa emissor `agendamento-smart-api` e expiração de duas horas.

#### Scenario: Credenciais válidas

- **WHEN** um usuário envia credenciais válidas para `POST /auth/login`
- **THEN** recebe `token` e a entidade `user` no corpo, além do cookie `jwt`

### Requirement: Requisições autenticadas aceitam token

O filtro MUST ler `Authorization` ou cookie `jwt`, validar o token e carregar o usuário pelo login. Se ambos são fornecidos, o cookie prevalece no código atual. Rotas não liberadas explicitamente exigem autenticação.

#### Scenario: Token válido

- **WHEN** uma requisição protegida contém JWT válido
- **THEN** o principal é carregado e a requisição segue autenticada

### Requirement: Perfil e listagem de usuários

`GET /auth/me` MUST devolver `UserProfileResponse` com usuário, clínica, tenant e primeira unidade ordenada por nome. `GET /auth/list` MUST listar todos os usuários para `ADMIN` ou `USER`; a consulta atual não filtra por clínica.

#### Scenario: Consulta de perfil

- **WHEN** um usuário autenticado chama `GET /auth/me`
- **THEN** recebe identificadores de sua clínica, tenant e unidade ativa

#### Scenario: Listagem de usuários

- **WHEN** um usuário `USER` chama `GET /auth/list`
- **THEN** a consulta atual retorna a lista global do repositório

### Requirement: Cadastro legado de usuário

`POST /user/register/{clinicId}` MUST localizar a clínica informada, rejeitar login existente, codificar senha com BCrypt e persistir o papel enviado. A rota exige autenticação, mas não verifica papel nem pertencimento à clínica e responde `200` sem corpo.

#### Scenario: Cadastro por usuário autenticado

- **WHEN** um usuário autenticado envia login novo, senha e papel para clínica existente
- **THEN** o usuário é persistido com senha codificada e a resposta não contém DTO

# Spec Delta

## MODIFIED Requirements

### Requirement: Perfil e listagem de usuários

`GET /auth/me` MUST devolver `UserProfileResponse` com usuário, clínica, tenant e unidade ativa autorizada. `GET /auth/list` MUST ser uma consulta administrativa da própria clínica, sem dados sensíveis; um usuário `USER` MUST não poder listar ou alterar o escopo de outros usuários.

#### Scenario: Consulta de perfil

- **WHEN** um usuário autenticado chama `GET /auth/me`
- **THEN** recebe identificadores e nomes de sua clínica e unidade associada, sem senha ou hash

#### Scenario: ADMIN consulta usuários

- **WHEN** um usuário `ADMIN` chama `GET /auth/list`
- **THEN** recebe somente usuários da própria clínica em uma representação segura, podendo aplicar o escopo de unidade permitido pela administração

#### Scenario: Listagem de usuários

- **WHEN** um usuário autenticado consulta a listagem administrativa de usuários
- **THEN** a consulta MUST aplicar a clínica e o papel do principal, sem retornar a lista global ou dados sensíveis

#### Scenario: USER tenta consultar usuários

- **WHEN** um usuário `USER` chama `GET /auth/list` ou tenta informar uma unidade para ampliar a consulta
- **THEN** a API MUST negar a operação e não MUST expor usuários de outra unidade ou clínica

### Requirement: Cada usuário pertence a uma clínica

Cada usuário MUST estar associado a exatamente uma clínica e, quando não for `ADMIN`, a uma unidade dessa mesma clínica para fins de escopo de consultas.

#### Scenario: Associação única

- **WHEN** uma conta autenticada for usada para consultar catálogos ou usuários
- **THEN** seu escopo MUST ser derivado da clínica e da unidade persistidas no principal, nunca de um identificador de unidade arbitrário enviado pelo cliente

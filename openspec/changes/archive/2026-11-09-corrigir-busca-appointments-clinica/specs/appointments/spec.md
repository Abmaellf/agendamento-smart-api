# Spec Delta

## MODIFIED Requirements

### Requirement: Consultas da agenda são delimitadas pela clínica

`GET /api/appointments/{id}` MUST localizar por ID e clínica do usuário autenticado. `GET /api/appointments` MUST exigir `from` e `to`, aceitar `unitId` opcional e listar no intervalo semiaberto `[from, to)`, ordenado por início, dentro de `content`. Sem `unitId`, MUST incluir todas as unidades da clínica autenticada. Com `unitId`, MUST validar que a unidade pertence a essa clínica e restringir a lista a ela. Um intervalo com `from` igual ou posterior a `to` MUST receber HTTP 400.

#### Scenario: Listagem por unidade e período

- **WHEN** usuário consulta unidade da própria clínica com `unitId`, `from` e `to`
- **THEN** recebe somente agendamentos daquela unidade e daquele intervalo em ordem de início dentro de `content`

#### Scenario: Listagem da clínica sem unidade

- **WHEN** usuário autenticado informa `from` e `to` e omite `unitId`
- **THEN** recebe em `content` os agendamentos de todas as unidades de sua clínica no intervalo, em ordem de início

#### Scenario: Unidade de outra clínica

- **WHEN** usuário informa `unitId` de uma unidade pertencente a outra clínica
- **THEN** a API responde `TENANT_RESOURCE_NOT_FOUND` com HTTP 404 e não expõe agendamentos da outra clínica

#### Scenario: Agendamento de outra clínica por ID

- **WHEN** usuário consulta `GET /api/appointments/{id}` com ID de agendamento de outra clínica
- **THEN** a API responde `TENANT_RESOURCE_NOT_FOUND` com HTTP 404

#### Scenario: Intervalo vazio

- **WHEN** usuário consulta um intervalo válido sem agendamentos na clínica ou unidade selecionada
- **THEN** a API responde HTTP 200 com `content` vazio

#### Scenario: Intervalo inválido

- **WHEN** usuário informa `from` igual ou posterior a `to`
- **THEN** a API responde HTTP 400

#### Scenario: Datas obrigatórias

- **WHEN** usuário omite `from` ou `to`
- **THEN** a API rejeita a consulta com HTTP 400

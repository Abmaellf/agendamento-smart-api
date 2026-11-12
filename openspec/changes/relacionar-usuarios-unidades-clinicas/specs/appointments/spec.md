# Spec Delta

## MODIFIED Requirements

### Requirement: Criação manual na clínica autenticada
`POST /api/appointments` MUST receber paciente, unidade, serviço, profissional opcional, início futuro, duração de 1 a 480 minutos e preço não negativo de até duas casas. A unidade MUST pertencer à clínica do principal e, para usuário não administrador, MUST ser a unidade associada ao usuário. O serviço registra autoria, duração, preço, fuso e início absoluto e responde HTTP 201.

#### Scenario: Criação por usuário não administrador na unidade associada
- **WHEN** um usuário não administrador envia recursos válidos da sua unidade associada
- **THEN** o agendamento é persistido em `AGENDADO` conforme as demais regras

#### Scenario: Criação válida
- **WHEN** usuário autenticado envia recursos válidos da unidade permitida, início futuro e parâmetros válidos
- **THEN** agendamento é persistido em `AGENDADO` com snapshots de duração e preço

#### Scenario: Criação por usuário não administrador em outra unidade
- **WHEN** um usuário não administrador informa unidade diferente da sua associação
- **THEN** a API impede a criação sem expor ou persistir recursos da outra unidade

#### Scenario: Recurso fora da clínica
- **WHEN** paciente, unidade, serviço ou profissional não pertence à clínica autenticada
- **THEN** a API responde `TENANT_RESOURCE_NOT_FOUND` com HTTP 404

#### Scenario: Criação por administrador
- **WHEN** um administrador envia recursos válidos de qualquer unidade da própria clínica
- **THEN** a criação segue as demais regras da agenda

### Requirement: Consultas da agenda são delimitadas pela clínica
`GET /api/appointments/{id}` MUST localizar por ID, clínica e unidade permitida ao usuário autenticado. `GET /api/appointments` MUST exigir `from` e `to`, aceitar `unitId` opcional e listar no intervalo `[from, to)` em `content`. Para não administrador, a resposta MUST conter somente a unidade associada, mesmo sem `unitId`, e não pode ser ampliada por parâmetro. Para `ADMIN`, a unidade solicitada MUST pertencer à própria clínica.

#### Scenario: Agenda de usuário não administrador
- **WHEN** usuário não administrador consulta a agenda no intervalo válido
- **THEN** recebe somente agendamentos da unidade associada

#### Scenario: Listagem por unidade e período
- **WHEN** usuário consulta unidade permitida com `unitId`, `from` e `to`
- **THEN** recebe somente agendamentos daquela unidade e intervalo em ordem de início dentro de `content`

#### Scenario: Listagem da clínica sem unidade
- **WHEN** administrador autenticado informa `from` e `to` e omite `unitId`
- **THEN** recebe em `content` os agendamentos permitidos de sua clínica no intervalo, em ordem de início

#### Scenario: Tentativa de ampliar unidade por parâmetro
- **WHEN** usuário não administrador informa `unitId` diferente da sua unidade
- **THEN** a API impede a exposição de agendamentos dessa unidade

#### Scenario: Agenda de administrador por unidade própria
- **WHEN** administrador consulta unidade da própria clínica com `unitId`
- **THEN** recebe somente agendamentos daquela unidade e intervalo

#### Scenario: Unidade de outra clínica
- **WHEN** usuário informa `unitId` de uma unidade pertencente a outra clínica
- **THEN** a API responde `TENANT_RESOURCE_NOT_FOUND` com HTTP 404 e não expõe agendamentos da outra clínica

#### Scenario: Agendamento de outra clínica por ID
- **WHEN** usuário consulta `GET /api/appointments/{id}` com ID de agendamento de outra clínica
- **THEN** a API responde `TENANT_RESOURCE_NOT_FOUND` com HTTP 404

#### Scenario: Intervalo vazio
- **WHEN** usuário consulta um intervalo válido sem agendamentos no escopo permitido
- **THEN** a API responde HTTP 200 com `content` vazio

#### Scenario: Intervalo inválido
- **WHEN** usuário informa `from` igual ou posterior a `to`
- **THEN** a API responde HTTP 400

#### Scenario: Datas obrigatórias
- **WHEN** usuário omite `from` ou `to`
- **THEN** a API rejeita a consulta com HTTP 400

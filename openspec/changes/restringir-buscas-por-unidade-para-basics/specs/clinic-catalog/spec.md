# Spec Delta

## MODIFIED Requirements

### Requirement: Catálogos usam a clínica autenticada

`GET /api/patients`, `/api/units` e `/api/services` MUST devolver `content` somente com registros da clínica do principal. Para `USER`, unidades e pacientes MUST ficar limitados à `clinic_unit` associada; para `ADMIN`, unidades e pacientes podem abranger todas as unidades da própria clínica. As opções continuam trazendo `tenantId` derivado da clínica autenticada quando aplicável.

#### Scenario: USER consulta unidades

- **WHEN** um usuário `USER` autenticado consultar `GET /api/units`
- **THEN** a resposta MUST conter somente sua unidade associada e não MUST aceitar parâmetro ou comportamento que amplie esse conjunto

#### Scenario: Usuário consulta opções

- **WHEN** usuário autenticado consulta qualquer catálogo
- **THEN** a consulta usa o ID de sua clínica e aplica o escopo de unidade correspondente ao papel

#### Scenario: ADMIN consulta unidades

- **WHEN** um usuário `ADMIN` autenticado consultar `GET /api/units`
- **THEN** a resposta MUST conter as unidades da própria clínica e nenhuma unidade de outra clínica

#### Scenario: USER consulta pacientes do catálogo

- **WHEN** um usuário `USER` autenticado consultar `GET /api/patients`
- **THEN** a resposta MUST conter somente pacientes vinculados à unidade associada

#### Scenario: Serviços permanecem no escopo da clínica

- **WHEN** qualquer usuário autenticado consultar `GET /api/services`
- **THEN** a resposta MUST conter somente serviços da clínica autenticada, sem transformar o `unitId` enviado pelo cliente em prova de autorização

### Requirement: Profissionais são filtrados por serviço

`GET /api/professionals?serviceId=...` MUST validar que o serviço pertence à clínica e listar somente profissionais da clínica que oferecem o serviço e pertencem a uma unidade autorizada pelo principal.

#### Scenario: USER consulta profissionais

- **WHEN** um usuário `USER` autenticado consultar profissionais de um serviço válido
- **THEN** a resposta MUST conter somente profissionais do serviço vinculados à unidade associada

#### Scenario: ADMIN consulta profissionais

- **WHEN** um usuário `ADMIN` autenticado consultar profissionais de um serviço válido
- **THEN** a resposta MUST conter profissionais do serviço em unidades da própria clínica

#### Scenario: Serviço de outra clínica

- **WHEN** `serviceId` não pertencer à clínica autenticada
- **THEN** a API MUST responder `TENANT_RESOURCE_NOT_FOUND` com HTTP 404

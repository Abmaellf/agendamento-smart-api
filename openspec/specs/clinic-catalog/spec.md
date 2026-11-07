# Clinic Catalog Specification

## Purpose

Consultas de opções e edição de serviço implementadas em `CatalogController` e `CatalogService`.

## Requirements

### Requirement: Catálogos usam a clínica autenticada

`GET /api/patients`, `/api/units` e `/api/services` MUST devolver `content` com registros da clínica do principal. As opções trazem `tenantId` derivado da primeira unidade ordenada por nome quando aplicável.

#### Scenario: Usuário consulta opções

- **WHEN** usuário autenticado consulta qualquer catálogo
- **THEN** a consulta usa o ID de sua clínica

### Requirement: Profissionais são filtrados por serviço

`GET /api/professionals?serviceId=...` MUST validar que o serviço pertence à clínica e listar profissionais da mesma clínica que o oferecem, incluindo dias disponíveis e IDs dos serviços.

#### Scenario: Serviço de outra clínica

- **WHEN** `serviceId` não pertence à clínica autenticada
- **THEN** a API responde `TENANT_RESOURCE_NOT_FOUND` com HTTP 404

### Requirement: Administrador altera padrões do serviço

`PUT /api/services/{id}` MUST exigir `ADMIN`, localizar o serviço na clínica do principal e atualizar duração e/ou preço quando enviados. Duração válida é de 1 a 480 minutos; preço é string decimal com duas casas.

#### Scenario: Alteração parcial

- **WHEN** administrador informa somente `durationMinutes` válido
- **THEN** a duração muda e o preço existente é preservado

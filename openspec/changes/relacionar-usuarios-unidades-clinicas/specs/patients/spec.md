# Spec Delta

## MODIFIED Requirements

### Requirement: Listagem paginada pública
`GET /patient/list` MUST exigir autenticação, aceitar paginação Spring Data e devolver `content`, `page`, `size`, `totalElements`, `totalPages` e `last`. Para usuário não administrador, MUST retornar somente pacientes vinculados à unidade associada. Para `ADMIN`, MUST retornar somente pacientes da clínica autenticada e, quando houver filtro de unidade permitido pelo contrato, somente da unidade solicitada pertencente à clínica.

#### Scenario: Usuário não administrador lista pacientes
- **WHEN** usuário não administrador autenticado chama `GET /patient/list`
- **THEN** recebe somente pacientes vinculados à sua unidade associada

#### Scenario: Visitante lista pacientes
- **WHEN** cliente não autenticado chama `GET /patient/list`
- **THEN** a API rejeita a requisição sem expor pacientes

#### Scenario: Administrador lista pacientes da clínica
- **WHEN** administrador autenticado chama `GET /patient/list`
- **THEN** recebe somente pacientes da sua clínica

#### Scenario: Cliente não autenticado lista pacientes
- **WHEN** cliente não autenticado chama `GET /patient/list`
- **THEN** a API rejeita a requisição sem expor pacientes

#### Scenario: Tentativa de consultar outra unidade
- **WHEN** usuário não administrador tenta indicar uma unidade diferente da sua associação
- **THEN** a API impede a exposição de pacientes dessa unidade

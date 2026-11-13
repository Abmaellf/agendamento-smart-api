# Spec Delta

## MODIFIED Requirements

### Requirement: Listagem paginada pública

`GET /patient/list` MUST exigir autenticação e devolver `content`, `page`, `size`, `totalElements`, `totalPages` e `last` somente com pacientes da clínica do principal. Para `USER`, a página MUST ficar limitada à unidade associada; para `ADMIN`, pode abranger as unidades da própria clínica.

#### Scenario: USER lista pacientes

- **WHEN** um usuário `USER` autenticado chama `GET /patient/list`
- **THEN** recebe uma página contendo somente pacientes vinculados à sua unidade associada

#### Scenario: ADMIN lista pacientes

- **WHEN** um usuário `ADMIN` autenticado chama `GET /patient/list`
- **THEN** recebe uma página de pacientes da própria clínica e nenhum paciente de outra clínica

#### Scenario: Visitante lista pacientes

- **WHEN** cliente não autenticado chama `GET /patient/list`
- **THEN** a API MUST rejeitar a chamada e não MUST retornar pacientes

#### Scenario: Tentativa de ampliar escopo

- **WHEN** um usuário `USER` envia parâmetros de unidade ou paginação tentando obter pacientes de outra unidade
- **THEN** a resposta MUST continuar limitada à unidade associada ou rejeitar o pedido, sem expor dados fora do escopo

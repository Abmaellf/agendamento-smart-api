# Patients Specification

## Purpose

Cadastro e listagem legados observados em `PatientController`, `PatientService`, `PatientRepository` e `PatientMapper`.

## Requirements

### Requirement: Listagem paginada pública

`GET /patient/list` MUST aceitar paginação Spring Data e devolver `content`, `page`, `size`, `totalElements`, `totalPages` e `last`. A configuração atual permite acesso sem autenticação e a consulta não filtra por clínica.

#### Scenario: Visitante lista pacientes

- **WHEN** cliente não autenticado chama `GET /patient/list`
- **THEN** recebe página de pacientes de todas as clínicas

### Requirement: Cadastro de paciente por ADMIN

`POST /patient/save` MUST exigir `ADMIN`, usar a clínica do usuário autenticado e receber uma lista não vazia de `unitIds` dessa clínica. Se `clinicId` legado for enviado, MUST coincidir com a clínica autenticada. O serviço MUST converter o nome para maiúsculas e persistir o paciente com seus vínculos de unidade. A verificação atual de nome duplicado permanece global nesta fase.

#### Scenario: Novo paciente

- **WHEN** administrador envia nome ainda não encontrado e pelo menos uma unidade de sua clínica
- **THEN** recebe DTO com ID, nome em maiúsculas, ID da clínica autenticada e vínculos às unidades informadas

#### Scenario: Nome já encontrado

- **WHEN** administrador envia nome que já existe na base
- **THEN** o serviço lança `PatientAlreadyExistsException`

#### Scenario: Unidade de outra clínica

- **WHEN** administrador informa `unitId` de unidade pertencente a outra clínica
- **THEN** a API rejeita o cadastro sem criar vínculo ou paciente na outra clínica

#### Scenario: Nenhuma unidade

- **WHEN** administrador omite `unitIds` ou envia uma lista vazia
- **THEN** a API rejeita o cadastro sem criar paciente

### Requirement: Paciente pertence a uma clínica e a suas unidades

Cada paciente MUST pertencer a exatamente uma clínica e a uma ou mais unidades dessa clínica. Um paciente pode ser vinculado a várias unidades da mesma clínica, mas MUST NOT ser vinculado a unidade de outra clínica.

#### Scenario: Paciente em duas unidades da mesma clínica

- **WHEN** paciente é vinculado a duas unidades da clínica à qual pertence
- **THEN** os dois vínculos são mantidos sob essa única clínica

#### Scenario: Associação entre clínicas

- **WHEN** uma operação tenta acrescentar ao paciente uma unidade de clínica diferente
- **THEN** o novo vínculo é rejeitado e os vínculos válidos permanecem

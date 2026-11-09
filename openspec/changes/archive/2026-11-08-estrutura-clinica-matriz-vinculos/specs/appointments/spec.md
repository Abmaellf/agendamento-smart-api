# Spec Delta

## ADDED Requirements

### Requirement: Novo appointment exige clínica e unidade ativas

`POST /api/appointments` MUST criar um novo agendamento somente quando a clínica autenticada e a unidade escolhida estiverem ambas `ATIVO`. `INATIVO` e `BLOQUEADA` em qualquer uma delas MUST impedir a criação, inclusive para `ADMIN`.

#### Scenario: Clínica e unidade ativas

- **WHEN** usuário autenticado cria appointment válido em unidade `ATIVO` de clínica `ATIVO`
- **THEN** a criação segue as demais regras da agenda

#### Scenario: Clínica bloqueada

- **WHEN** usuário tenta criar appointment em clínica `BLOQUEADA`
- **THEN** a API rejeita a criação com HTTP 403 e não persiste novo agendamento

#### Scenario: Unidade inativa

- **WHEN** usuário tenta criar appointment em unidade `INATIVO` de sua clínica
- **THEN** a API rejeita a criação com HTTP 403 e não persiste novo agendamento

### Requirement: Appointment exige paciente e profissional da unidade

`POST /api/appointments` MUST exigir que o paciente esteja vinculado à unidade informada. O profissional permanece opcional; quando informado, MUST pertencer a essa mesma unidade. Vínculo apenas com a clínica, sem vínculo à unidade escolhida, não autoriza o agendamento.

#### Scenario: Paciente vinculado e profissional ausente

- **WHEN** paciente pertence à unidade escolhida e nenhum profissional é informado
- **THEN** a criação pode prosseguir sem profissional, sujeita às demais regras

#### Scenario: Paciente de outra unidade

- **WHEN** paciente pertence à clínica, mas não à unidade escolhida
- **THEN** a API rejeita a criação sem persistir appointment

#### Scenario: Profissional de outra unidade

- **WHEN** profissional informado pertence à clínica, mas está vinculado a outra unidade
- **THEN** a API rejeita a criação sem persistir appointment

### Requirement: Leitura histórica considera status da clínica e da unidade

`GET /api/appointments/{id}` e `GET /api/appointments` MUST impedir leitura de agendamentos de clínica ou unidade `INATIVO`. Clínica e unidade `BLOQUEADA` MUST permitir leitura histórica. Quando a listagem abranger várias unidades, MUST excluir as unidades `INATIVO`.

#### Scenario: Clínica inativa

- **WHEN** usuário consulta appointments de clínica `INATIVO`
- **THEN** a API rejeita a consulta com HTTP 403

#### Scenario: Unidade inativa por ID ou filtro

- **WHEN** usuário consulta por ID ou filtra a lista por unidade `INATIVO`
- **THEN** a API rejeita a consulta com HTTP 403

#### Scenario: Unidade bloqueada

- **WHEN** usuário consulta appointments antigos de unidade `BLOQUEADA` em clínica não inativa
- **THEN** a API permite a leitura conforme os filtros e permissões existentes

#### Scenario: Lista de várias unidades

- **WHEN** uma listagem por clínica abrange unidades `ATIVO`, `BLOQUEADA` e `INATIVO`
- **THEN** `content` contém somente agendamentos das unidades `ATIVO` e `BLOQUEADA`


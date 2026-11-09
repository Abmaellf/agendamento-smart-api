# Appointments Specification

## Purpose

Agenda manual observada em `AppointmentController`, `AppointmentService`, DTOs de appointment e migrations `V008`/`V009`.

## Requirements

### Requirement: Criação manual na clínica autenticada

`POST /api/appointments` MUST receber paciente, unidade, serviço, profissional opcional, início futuro, duração de 1 a 480 minutos e preço não negativo de até duas casas. O serviço resolve vínculos na clínica do principal, atribui `AGENDADO`, registra autoria, duração, preço, fuso e início absoluto. O controller responde HTTP 201.

#### Scenario: Criação válida

- **WHEN** usuário autenticado envia recursos da própria clínica, início futuro e parâmetros válidos
- **THEN** agendamento é persistido em `AGENDADO` com snapshots de duração e preço

#### Scenario: Recurso fora da clínica

- **WHEN** paciente, unidade, serviço ou profissional não pertence à clínica autenticada
- **THEN** a API responde `TENANT_RESOURCE_NOT_FOUND` com HTTP 404

### Requirement: Alteração dos padrões do serviço é restrita

Usuário `USER` MUST informar duração e preço iguais aos padrões do serviço. `ADMIN` pode sobrescrevê-los no agendamento.

#### Scenario: Usuário comum altera preço

- **WHEN** usuário `USER` solicita preço diferente do serviço
- **THEN** a API responde `APPOINTMENT_OVERRIDE_FORBIDDEN` com HTTP 422

### Requirement: Disponibilidade e capacidade são verificadas na criação

O serviço MUST recusar início passado, profissional que não oferece o serviço ou está indisponível no dia local da unidade, sobreposição de paciente, sobreposição de profissional e capacidade simultânea esgotada do serviço. A criação bloqueia pessimisticamente a clínica durante verificação e gravação. Somente `AGENDADO` e `ATENDENDO` contam como ativos nessas verificações.

#### Scenario: Paciente com intervalo conflitante

- **WHEN** paciente já possui agendamento ativo sobreposto
- **THEN** a API responde `PATIENT_APPOINTMENT_CONFLICT` com HTTP 409

#### Scenario: Profissional com intervalo conflitante

- **WHEN** profissional já possui agendamento ativo sobreposto
- **THEN** a API responde `PROFESSIONAL_APPOINTMENT_CONFLICT` com HTTP 409

#### Scenario: Capacidade esgotada

- **WHEN** agendamentos ativos sobrepostos alcançam capacidade do serviço
- **THEN** a API responde `SERVICE_CAPACITY_EXCEEDED` com HTTP 409

### Requirement: Chave de idempotência pode reutilizar criação

Quando `Idempotency-Key` não vazia é enviada, o serviço MUST procurar agendamento anterior com mesma chave na clínica e devolver seu conteúdo. A chave persistida é limitada a 120 caracteres pelo código atual; o banco tem restrição única por clínica e chave.

#### Scenario: Repetição da chave

- **WHEN** a mesma chave é enviada novamente na mesma clínica
- **THEN** o serviço devolve agendamento já persistido

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

### Requirement: Estado atual não possui transições HTTP

O enum persistido admite `AGENDADO`, `ATENDENDO`, `CANCELADO` e `FINALIZADO`; a API MUST expor, nesta linha de base, apenas criação e leitura, sem endpoints para mudar estado, cancelar, remarcar ou criar recorrência.

#### Scenario: Cliente consulta operações da agenda

- **WHEN** procura as operações implementadas
- **THEN** encontra apenas `POST` de criação e `GET` de listagem e detalhe

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

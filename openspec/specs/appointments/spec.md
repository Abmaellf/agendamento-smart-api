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

`GET /api/appointments/{id}` MUST localizar por ID e clínica. `GET /api/appointments` MUST exigir `unitId`, `from` e `to`, validar unidade na clínica e listar no intervalo semiaberto `[from, to)`, ordenado por início, dentro de `content`.

#### Scenario: Listagem por unidade e período

- **WHEN** usuário consulta unidade da própria clínica com `from` e `to`
- **THEN** recebe agendamentos daquele intervalo em ordem de início

### Requirement: Estado atual não possui transições HTTP

O enum persistido admite `AGENDADO`, `ATENDENDO`, `CANCELADO` e `FINALIZADO`; a API MUST expor, nesta linha de base, apenas criação e leitura, sem endpoints para mudar estado, cancelar, remarcar ou criar recorrência.

#### Scenario: Cliente consulta operações da agenda

- **WHEN** procura as operações implementadas
- **THEN** encontra apenas `POST` de criação e `GET` de listagem e detalhe

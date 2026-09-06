# Modelo de agendamento

## Objetivo do módulo

Representar um agendamento de paciente e mapear a lista de patologias entre Java e `jsonb` PostgreSQL.

## Responsabilidades principais

- Mapear a tabela `APPOINTMENT`.
- Relacionar obrigatoriamente o agendamento a `Patient`.
- Persistir patologias em coluna `jsonb` com `@JdbcTypeCode(SqlTypes.JSON)`.
- Representar status e timestamps de criação/atualização.

## Funcionalidades existentes

- Entidade `Appointment`.
- Status Java `AGENDADO`, `ATENDENDO`, `CANCELADO` e `FINALIZADO`.
- Mapeamento Hibernate JSON para `List<String>`.
- `UUID` nativo para IDs e `timestamptz` para `starts_at`, `created_at` e `updated_at`.
- Atualização de `updatedAt` por `@PreUpdate`.

## Dependências internas e externas

- Internas: `model/patient/Patient`.
- Externas: JPA, Jackson, Hibernate e Lombok.

## Módulos relacionados

`controller/appointment`, `service/appointment`, `repository`, DTOs de agendamento e migrations `V008`/`V009`.

## Pontos de entrada

- Construção pelo `AppointmentService` no fluxo de criação.
- Materialização pelo JPA no fluxo de consulta.
- Hibernate chamado pelo provedor JPA para gravar/ler `pathology` como `jsonb`.

## Fluxos de entrada

Request -> service -> associações de domínio -> entidade -> Hibernate/JPA -> PostgreSQL; caminho inverso na consulta.

## Arquivos críticos

- `Appointment.java`.
- `AppointmentStatus.java`.

## Regras confirmadas para evolução do módulo

- O conceito canônico é `Appointment`: um registro por paciente, vinculado a tenant, unidade, serviço e profissional opcional.
- O agendamento guarda início com fuso explícito e snapshots de duração e preço; profissional é obrigatório antes de `EM_ATENDIMENTO` ou `CONCLUIDO`.
- Estados canônicos: `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO`.
- Sobreposição do mesmo paciente, sobreposição do mesmo profissional e capacidade excedida do serviço são sempre proibidas.
- Ocorrências recorrentes são independentes e ligadas a `AppointmentSeries`.
- Remarcação mantém o original como `REMARCADO`, cria um novo registro vinculado e audita autor, instante e motivo. Cancelamento também é auditável.
- `AppointmentEvent` preserva o histórico e nenhuma FK pode apagar agendamento/evento em cascata.
- `pathology` e `variant` não fazem parte do modelo mínimo confirmado; `appointmentDate` + `hours` deve ser substituído por uma representação temporal única.

## Observações técnicas e débitos identificados

- A evolução de `AppointmentStatus` para todos os estados canônicos exige migration compatível com registros existentes.
- `appointmentDate` e `hours` podem representar horas diferentes e não há validação.
- Não existem regras ou comandos de transição de status; conflitos, duração e data futura são validados na criação pelo service.
- Timestamps são inicializados pela aplicação e também possuem defaults na migration; não há uma fonte temporal única declarada.
- As FKs de `APPOINTMENT` usam exclusão restritiva para preservar o histórico.

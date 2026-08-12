# Modelo de agendamento

## Objetivo do módulo

Representar um agendamento de paciente e converter a lista de patologias entre Java e JSON MySQL.

## Responsabilidades principais

- Mapear a tabela `SCHEDULING`.
- Relacionar obrigatoriamente o agendamento a `Patient`.
- Persistir patologias em coluna JSON.
- Representar status e timestamps de criação/atualização.

## Funcionalidades existentes

- Entidade `Scheduling`.
- Status Java `AGENDADO`, `ATENDENDO`, `CANCELADO` e `FINALIZADO`.
- `StringListJsonConverter` para `List<String>`.
- Atualização de `updatedAt` por `@PreUpdate`.

## Dependências internas e externas

- Internas: `model/patient/Patient`.
- Externas: JPA, Jackson, Hibernate e Lombok.

## Módulos relacionados

`controller/scheduling`, `service/scheduling`, `repository`, `mapper`, DTOs de agendamento e migrations `V004`/`V005`.

## Pontos de entrada

- Construção pelo `SchedulingMapper` no fluxo de criação.
- Materialização pelo JPA no fluxo de consulta.
- Conversor chamado pelo provedor JPA ao gravar/ler `pathology`.

## Fluxos de entrada

Request -> mapper -> associação do paciente -> entidade -> conversor JSON -> MySQL; caminho inverso na consulta.

## Arquivos críticos

- `Scheduling.java`.
- `StatusScheduling.java`.
- `StringListJsonConverter.java`.

## Regras confirmadas para evolução do módulo

- O conceito canônico é `Appointment`: um registro por paciente, vinculado a tenant, unidade, serviço e profissional opcional.
- O agendamento guarda início com fuso explícito e snapshots de duração e preço; profissional é obrigatório antes de `EM_ATENDIMENTO` ou `CONCLUIDO`.
- Estados canônicos: `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO`.
- Sobreposição do mesmo paciente, sobreposição do mesmo profissional e capacidade excedida do serviço são sempre proibidas.
- Ocorrências recorrentes são independentes e ligadas a `AppointmentSeries`.
- Remarcação mantém o original como `REMARCADO`, cria um novo registro vinculado e audita autor, instante e motivo. Cancelamento também é auditável.
- `AppointmentEvent` preserva o histórico e nenhuma FK pode apagar agendamento/evento em cascata.
- `pathology` e `variant` não fazem parte do modelo mínimo confirmado; `dateScheduling` + `hours` deve ser substituído por uma representação temporal única.

## Observações técnicas e débitos identificados

- `ATENDENDO` não existe no `ENUM` MySQL criado por `V004`; seu uso falha na persistência.
- O mapper pode definir status nulo e remover o default `AGENDADO`.
- `dateScheduling` e `hours` podem representar horas diferentes e não há validação.
- Não existem regras de transição de status, conflito, duração ou data futura.
- O conversor desserializa `List.class` sem tipo genérico explícito e descarta a causa original ao lançar `IllegalArgumentException`.
- Timestamps são inicializados pela aplicação e também possuem defaults na migration; não há uma fonte temporal única declarada.
- A migration apaga agendamentos ao excluir paciente (`ON DELETE CASCADE`).

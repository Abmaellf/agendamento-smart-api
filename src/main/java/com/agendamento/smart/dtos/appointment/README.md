# DTOs de agendamento

## Objetivo do módulo

Definir os contratos de criação e leitura de agendamentos.

## Responsabilidades principais

- Transportar o UUID do paciente e os dados do agendamento na entrada.
- Expor IDs, dados de agenda, status e timestamps na saída.

## Funcionalidades existentes

- `AppointmentRequest` exige `patientId`, `unitId`, `serviceId`, `startsAt`, `durationMinutes` e `price`; `professionalId` é opcional.
- `AppointmentResponse` representa o agendamento persistido.

## Dependências internas e externas

- Internas: `model/appointment/AppointmentStatus`.
- Externas: Jakarta Validation e tipos Java de UUID/data/hora/lista.

## Módulos relacionados

`controller/appointment`, `service/appointment` e `model/appointment`.

## Pontos de entrada

- Corpo de `POST /api/appointments`.
- Corpo de resposta de criação e `GET /api/appointments` ou `GET /api/appointments/{id}`.

## Fluxos de entrada

JSON -> `AppointmentRequest` -> service -> entidade; entidade -> service -> `AppointmentResponse` -> JSON.

## Arquivos críticos

- `AppointmentRequest.java`.
- `AppointmentResponse.java`.

## Regras confirmadas para evolução do módulo

- O comando-alvo de criação contém `patientId`, `unitId`, `serviceId`, início com fuso explícito e profissional opcional; overrides de duração/preço são opcionais e exclusivos de `ADMIN`.
- O servidor determina duração e preço aplicados a partir do serviço ou dos overrides autorizados, e a resposta os expõe como snapshots.
- O status inicial é definido pelo servidor como `AGENDADO`; clientes não podem escolher livremente um estado na criação.
- Respostas usam os estados canônicos e incluem vínculos de série/remarcação e metadados de auditoria quando existentes.
- Recorrência, remarcação, cancelamento e transição de estado devem usar contratos de comando próprios, em vez de uma atualização genérica irrestrita.
- `pathology`, `hours` e `variant` pertencem ao contrato legado e não correspondem aos campos mínimos confirmados do MVP.

## Observações técnicas e débitos identificados

- O pacote canônico é `dtos.appointment`, alinhado a controller, service e model.
- `appointmentDate` contém data e hora e coexiste com `hours`, sem regra de consistência.
- Os DTOs dependem diretamente do enum persistente.
- Não há constraints de tamanho/conteúdo para patologias, validação de data futura ou valores aceitos para `variant`.

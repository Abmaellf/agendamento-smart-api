# DTOs de agendamento

## Objetivo do módulo

Definir os contratos de criação e leitura de agendamentos.

## Responsabilidades principais

- Transportar o UUID do paciente e os dados do agendamento na entrada.
- Expor IDs, dados de agenda, status e timestamps na saída.

## Funcionalidades existentes

- `SchedulingRequest` exige `patientId`, `pathology`, `dateScheduling` e `hours` não nulos.
- `SchedulingResponse` representa o agendamento persistido.

## Dependências internas e externas

- Internas: `model/scheduling/StatusScheduling`.
- Externas: Jakarta Validation e tipos Java de UUID/data/hora/lista.

## Módulos relacionados

`controller/scheduling`, `service/scheduling`, `mapper` e `model/scheduling`.

## Pontos de entrada

- Corpo de `POST /api/scheduling`.
- Corpo de resposta de criação e `GET /api/scheduling/{id}`.

## Fluxos de entrada

JSON -> `SchedulingRequest` -> MapStruct -> entidade; entidade -> MapStruct -> `SchedulingResponse` -> JSON.

## Arquivos críticos

- `SchedulingRequest.java`.
- `SchedulingResponse.java`.

## Regras confirmadas para evolução do módulo

- O comando-alvo de criação contém `patientId`, `unitId`, `serviceId`, início com fuso explícito e profissional opcional; overrides de duração/preço são opcionais e exclusivos de `ADMIN`.
- O servidor determina duração e preço aplicados a partir do serviço ou dos overrides autorizados, e a resposta os expõe como snapshots.
- O status inicial é definido pelo servidor como `AGENDADO`; clientes não podem escolher livremente um estado na criação.
- Respostas usam os estados canônicos e incluem vínculos de série/remarcação e metadados de auditoria quando existentes.
- Recorrência, remarcação, cancelamento e transição de estado devem usar contratos de comando próprios, em vez de uma atualização genérica irrestrita.
- `pathology`, `hours` e `variant` pertencem ao contrato legado e não correspondem aos campos mínimos confirmados do MVP.

## Observações técnicas e débitos identificados

- O pacote se chama `agendamento`, enquanto controller, service e model usam `scheduling`.
- O status de entrada não é obrigatório, mas o mapper atribui o valor nulo à entidade e remove o default `AGENDADO`.
- `dateScheduling` contém data e hora e coexiste com `hours`, sem regra de consistência.
- Os DTOs dependem diretamente do enum persistente.
- Não há constraints de tamanho/conteúdo para patologias, validação de data futura ou valores aceitos para `variant`.

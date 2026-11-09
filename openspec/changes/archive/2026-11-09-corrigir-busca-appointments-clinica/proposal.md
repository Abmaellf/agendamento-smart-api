# Proposal

## Why

A busca de agendamentos relatada pelo usuário não retorna o resultado esperado. Por inspeção, `GET /api/appointments` exige `unitId`, `from` e `to`, e a consulta limita os registros a uma única unidade; ainda não houve reprodução HTTP para determinar qual condição explica o caso relatado.

## What Changes

- Reproduzir e registrar, somente no fluxo de appointments, as respostas para parâmetros ausentes, unidade inválida ou alheia, intervalo sem registros e consulta válida com registros.
- Permitir consultar os agendamentos de todas as unidades da clínica do usuário autenticado quando `unitId` não for enviado; manter `unitId` como filtro opcional por unidade da mesma clínica.
- Manter `from` e `to` obrigatórios, o intervalo semiaberto `[from, to)`, a ordenação por início e o envelope `content`; definir resposta verificável para intervalo inválido.
- Verificar autorização e isolamento entre clínicas nas buscas por lista e por ID.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `appointments`: a listagem passa a aceitar consulta por clínica autenticada sem `unitId` e preserva a busca filtrada por unidade.

## Impact

Afeta o contrato de `GET /api/appointments`, `AppointmentController`, `AppointmentService`, `AppointmentRepository` e testes do módulo. `User.clinic` identifica a clínica autenticada; `ClinicUnit.clinic` valida o filtro de unidade. Não requer alteração de schema, do catálogo, de autenticação, do front-end, de criação de agendamento ou de transições de estado.

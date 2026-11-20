# API de appointments

Este documento descreve o contrato implementado para criação e leitura da
agenda, incluindo a interação entre `StatusClinic` e `StatusUnit`.

Todas as rotas usam exclusivamente a clínica do usuário autenticado como
fronteira de acesso. Um ID pertencente a outra clínica responde `404` com
`TENANT_RESOURCE_NOT_FOUND`, sem revelar o recurso.

## Matriz de permissões por status

O login é permitido em todas as combinações. A criação exige clínica e unidade
`ATIVO`. A leitura histórica é bloqueada somente por `INATIVO`; `BLOQUEADA`
impede novos agendamentos, mas preserva a leitura.

| Clínica | Unidade | Login | Criar | Ler por ID/filtro explícito |
|---|---|---:|---:|---:|
| `ATIVO` | `ATIVO` | Sim | Sim | Sim |
| `ATIVO` | `BLOQUEADA` | Sim | Não | Sim |
| `ATIVO` | `INATIVO` | Sim | Não | Não |
| `BLOQUEADA` | `ATIVO` | Sim | Não | Sim |
| `BLOQUEADA` | `BLOQUEADA` | Sim | Não | Sim |
| `BLOQUEADA` | `INATIVO` | Sim | Não | Não |
| `INATIVO` | qualquer status | Sim | Não | Não |

Precedência aplicada:

1. a clínica do principal é resolvida e validada antes da unidade;
2. na criação, qualquer status diferente de `ATIVO` bloqueia a operação;
3. na leitura, clínica `INATIVO` bloqueia toda consulta;
4. com clínica legível, unidade `INATIVO` bloqueia detalhe e filtro explícito;
5. `BLOQUEADA` nunca supera um `INATIVO`, mas permite leitura quando não há
   nenhuma parte inativa.

Na listagem ampla por clínica, não existe erro por haver uma unidade inativa:
seus appointments são excluídos de `content`. Unidades `ATIVO` e `BLOQUEADA`
continuam presentes.

## Criar appointment

### `POST /api/appointments`

Além da matriz de status, o paciente deve pertencer à unidade escolhida. O
profissional é opcional; quando informado, deve pertencer à mesma unidade e
oferecer o serviço.

Exemplo sem profissional, correspondente ao cenário HTTP de criação opcional:

```json
{
  "patientId": "88888888-8888-4888-8888-888888888888",
  "unitId": "22222222-2222-4222-8222-222222222222",
  "serviceId": "99999999-9999-4999-8999-999999999999",
  "professionalId": null,
  "startsAt": "2026-11-18T14:00:00Z",
  "durationMinutes": 30,
  "price": 125.00
}
```

Resposta: `201 Created`, com `professional: null` e status inicial `AGENDADO`.

Erros estruturais relevantes:

| HTTP | `code` | Situação |
|---|---|---|
| `403` | `CLINIC_APPOINTMENTS_DISABLED` | clínica `INATIVO` ou `BLOQUEADA` |
| `403` | `UNIT_APPOINTMENTS_DISABLED` | unidade `INATIVO` ou `BLOQUEADA` |
| `422` | `PATIENT_UNIT_MISMATCH` | paciente da clínica sem vínculo com a unidade |
| `422` | `PROFESSIONAL_UNIT_MISMATCH` | profissional da clínica vinculado a outra unidade |

Esses bloqueios são aplicados também a `ADMIN` e não persistem appointment.

## Listar appointments

### `GET /api/appointments`

`from` e `to` são instantes UTC obrigatórios. O intervalo é semiaberto
`[from,to)`, e `content` vem ordenado por `startsAt` crescente.

Listagem de todas as unidades legíveis da clínica:

```http
GET /api/appointments?from=2026-11-18T14:00:00Z&to=2026-11-18T17:00:00Z
Authorization: Bearer <token>
```

No cenário integrado, appointments às `14:00` em unidade `ATIVO` e às `15:00`
em unidade `BLOQUEADA` aparecem nessa ordem; o das `16:00` em unidade
`INATIVO` e o exatamente às `17:00` não aparecem.

Filtro compatível por uma unidade da mesma clínica:

```http
GET /api/appointments?unitId=22222222-2222-4222-8222-222222222222&from=2026-11-18T14:00:00Z&to=2026-11-18T17:00:00Z
Authorization: Bearer <token>
```

Trecho ilustrativo da resposta (o DTO contém também os demais campos do
appointment):

```json
{
  "content": [
    {
      "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
      "unitId": "22222222-2222-4222-8222-222222222222",
      "startsAt": "2026-11-18T14:00:00.000Z",
      "status": "AGENDADO"
    }
  ]
}
```

Erros relevantes:

| HTTP | `code` | Situação |
|---|---|---|
| `400` | `INVALID_APPOINTMENT_RANGE` | `from` igual ou posterior a `to` |
| `400` | resposta padrão do Spring | `from` ou `to` ausente/malformado |
| `403` | `CLINIC_APPOINTMENTS_READ_DISABLED` | clínica `INATIVO` |
| `403` | `UNIT_APPOINTMENTS_READ_DISABLED` | filtro explícito para unidade `INATIVO` |
| `404` | `TENANT_RESOURCE_NOT_FOUND` | unidade pertence a outra clínica ou não existe |

## Iniciar appointments da hora corrente em lote

### `POST /api/appointments/start-current-hour`

O comando confirma a presença dos appointments da unidade na hora corrente,
usando o fuso IANA persistido em `ClinicUnit.timeZone`. O corpo aceita somente
`unitId` e `hourStart`; `hourStart` deve ser o início da hora local corrente,
serializado como instante ISO-8601. A faixa consultada é semiaberta
`[hourStart, hourStart + 1h)`.

```json
{
  "unitId": "22222222-2222-4222-8222-222222222222",
  "hourStart": "2026-11-20T14:00:00Z"
}
```

Somente `AGENDADO` e `FALTA` são elegíveis e passam a `ATENDENDO`. A inclusão
de `FALTA` representa confirmação explícita de que a pessoa está no local;
`CANCELADO`, `FINALIZADO` e demais estados são preservados. A operação valida
a clínica do usuário, o vínculo da unidade, o escopo do usuário não
administrador, o status legível de clínica/unidade e os mesmos conflitos de
paciente, profissional e capacidade aplicados à criação. A seleção é
bloqueada por unidade/faixa e a transação só confirma todos os elegíveis se a
validação terminar sem conflito.

Resposta `200 OK`:

```json
{
  "unitId": "22222222-2222-4222-8222-222222222222",
  "timeZone": "America/Cuiaba",
  "hourStart": "2026-11-20T14:00:00Z",
  "hourEnd": "2026-11-20T15:00:00Z",
  "evaluatedAt": "2026-11-20T14:32:00Z",
  "targetStatus": "ATENDENDO",
  "matchedCount": 4,
  "updatedCount": 3,
  "skippedCount": 1,
  "skippedByStatus": { "CANCELADO": 1 }
}
```

Erros relevantes: `400/INVALID_APPOINTMENT_HOUR`, `403` para clínica ou
unidade sem leitura, `404/TENANT_RESOURCE_NOT_FOUND` para escopo inválido,
`409/APPOINTMENT_HOUR_NOT_CURRENT` para faixa expirada/futura e conflitos de
reativação, e `422/UNIT_TIME_ZONE_INVALID` para fuso inválido.

## Alterar status

### `PATCH /api/appointments/{id}/status`

O corpo aceita somente os comandos manuais `ATENDENDO` e `CANCELADO` quando o
appointment está em `AGENDADO` e pertence à clínica do usuário autenticado.
Usuários não administradores também precisam estar vinculados à unidade do
appointment.

```json
{
  "status": "ATENDENDO"
}
```

Em caso de sucesso, a API responde `200 OK` com o `AppointmentResponse`
atualizado. O comando não aceita `FALTA`, pois esse estado é reservado à
reconciliação automática.

| HTTP | `code` | Situação |
|---|---|---|
| `400` | `VALIDATION_ERROR` | corpo ausente, status nulo ou status não aceito |
| `404` | `TENANT_RESOURCE_NOT_FOUND` | appointment de outra clínica/unidade autorizada |
| `409` | `APPOINTMENT_STATUS_TRANSITION_INVALID` | status atual não permite a transição |
| `409` | `APPOINTMENT_NO_SHOW_DEADLINE` | início/cancelamento após dez minutos do início |

## Falta automática e monitoramento

Um appointment em `AGENDADO` passa a `FALTA` quando `startsAt + 10 minutos`
for atingido sem confirmação de `ATENDENDO`. A regra usa o instante absoluto
persistido em `starts_at`; não depende do fuso do processo nem de um timer do
frontend.

A API executa uma reconciliação periódica configurável, habilitada por padrão,
com intervalo de 60 segundos (`appointments.status-monitor.fixed-delay-ms`) e
atraso inicial configurável (`appointments.status-monitor.initial-delay-ms`). As leituras de lista e detalhe também reconciliam
appointments vencidos antes de montar a resposta, reduzindo a janela entre o
limite e o estado observado. A atualização é condicional ao status ainda ser
`AGENDADO`, portanto pode ser repetida em várias instâncias sem reverter
`ATENDENDO`, `CANCELADO` ou `FINALIZADO`.

Cada rodada registra somente informações operacionais: identificador da rodada,
instante/duração, quantidade de candidatos, quantidade atualizada e tipo de
erro. Pacientes, tokens, payloads e identificadores de appointments não entram
nos logs. Alertas operacionais podem considerar duas janelas consecutivas sem
rodada concluída ou uma falha registrada.

## Consultar por ID

### `GET /api/appointments/{id}`

O ID é sempre combinado com a clínica autenticada. Appointment de outra
clínica ou inexistente responde `404/TENANT_RESOURCE_NOT_FOUND`. Se a clínica
ou a unidade do registro estiver `INATIVO`, a resposta é `403` com o mesmo
código de leitura da tabela anterior; registros em `BLOQUEADA` permanecem
consultáveis.

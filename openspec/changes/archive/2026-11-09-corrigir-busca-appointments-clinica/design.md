# Design

## Context

Ver `proposal.md` e `specs/appointments/spec.md`. Hoje `AppointmentController.findAll` exige os três parâmetros. `AppointmentService.findAll` deriva `clinicId` de `User.clinic`, valida `unitId` com `UnitRepository.findByIdAndClinicId` e consulta `AppointmentRepository` por clínica, unidade e `startsAt`. `ClinicUnit.clinic` já representa o vínculo da unidade; `User` não possui unidade própria. `/auth/me` seleciona a primeira unidade por nome, mas isso não identifica todos os agendamentos da clínica. As seeds de desenvolvimento incluem duas unidades e agendamentos em ambas. Não há teste automatizado de appointments neste checkout nem reprodução HTTP registrada para a falha relatada.

## Goals / Non-Goals

**Goals:**

- Fazer a clínica do principal autenticado ser a única origem do `clinicId` aplicado às duas modalidades de listagem.
- Preservar compatibilidade com clientes que enviam `unitId` e permitir consulta por clínica quando o filtro não vier.
- Ter evidência reproduzível do comportamento antes e depois da implementação.

**Non-Goals:**

- Alterar `/auth/me`, `/api/units`, o modelo de associação de usuários a unidades ou o front-end.
- Introduzir paginação, mudar a criação de appointments ou consultar por data local sem instante UTC.

## Decisions

### Clínica do principal; unidade como filtro

O service obtém `clinicId` de `user.getClinic().getId()`. Quando `unitId` é informado, verifica o par `(unitId, clinicId)` antes da consulta. Quando ausente, consulta por `clinicId` e intervalo, sem escolher uma unidade arbitrária. Seguir o vínculo `ClinicUnit.clinic` para descobrir uma clínica a partir de `unitId` foi descartado: o parâmetro pertence ao cliente e não deve definir a fronteira de autorização. Usar a primeira unidade de `/auth/me` também foi descartado porque ocultaria appointments das demais unidades.

### Duas consultas delimitadas por tenant

Acrescentar ao repository uma consulta por clínica e `startsAt` no intervalo, ordenada por início. Manter a consulta existente por clínica e unidade quando `unitId` estiver presente. As duas preservam o envelope `CatalogResponse.content` e a semântica `[from, to)`. Uma consulta global com filtro posterior em memória foi descartada por custo e risco de exposição entre clínicas.

### Validação do intervalo

`from` e `to` continuam sendo `Instant` obrigatórios. O service rejeita `from >= to` com HTTP 400 antes de consultar o repository. Parâmetros omitidos ou mal formatados permanecem sob a validação de entrada do Spring. Não converter datas locais implicitamente, pois a unidade pode ter fuso próprio.

## Risks / Trade-offs

- [Sem `unitId`, a resposta pode crescer com o número de unidades] → Usar filtro obrigatório de período e medir o volume na verificação integrada; paginação fica para mudança própria se necessária.
- [A causa relatada pode estar na chamada do cliente ou nos dados, e não apenas na obrigatoriedade de `unitId`] → Reproduzir a requisição real ou um caso equivalente antes de alterar o código, registrar status, parâmetros e resultado sem dados pessoais, e comparar com as seeds e a consulta SQL efetiva.
- [Unidade de outra clínica] → Manter validação do par unidade/clínica e teste de isolamento; nunca inferir a clínica do `unitId` recebido.

## Migration Plan

Não há migration de banco nem quebra para chamadas que já enviam `unitId`. Publicar a API após testes do contrato e teste integrado com duas unidades e duas clínicas. Em rollback, restaurar a versão anterior da API; os dados não são alterados.

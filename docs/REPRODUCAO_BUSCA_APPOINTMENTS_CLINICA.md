# Reprodução da busca de appointments por clínica

## Objetivo

Registrar, sem dados pessoais, o comportamento HTTP anterior à change
`corrigir-busca-appointments-clinica`. A reprodução separa:

- erro de entrada por parâmetro obrigatório ausente;
- filtro explícito por unidade;
- intervalo válido sem registros;
- intervalo inválido;
- unidade que não pertence ao tenant autenticado ou não existe.

## Ambiente da linha de base

A medição foi executada em 2026-11-08 a partir de uma exportação temporária do
commit `cfc462375ee18801307a648860fc0458e1a4969b`, com PostgreSQL 18 descartável,
migrations `V001` a `V009` e os seeds de desenvolvimento `R__001` a `R__008`.
O código da working tree não foi usado nessa execução.

Parâmetros técnicos utilizados:

- intervalo amplo: `from=2020-01-01T00:00:00Z` e
  `to=2035-01-01T00:00:00Z`;
- intervalo sem registros: `from=2035-01-01T00:00:00Z` e
  `to=2036-01-01T00:00:00Z`;
- unidade 1: `60000000-0000-4000-8000-000000000001`;
- unidade 2: `60000000-0000-4000-8000-000000000002`;
- unidade inexistente: `ffffffff-ffff-4fff-8fff-ffffffffffff`.

## Resultados observados antes da correção

| Caso | Status | Resultado técnico observado |
| --- | ---: | --- |
| Sem `unitId`, com `from`/`to` válidos | 400 | `Required parameter 'unitId' is not present.` |
| Unidade 1, intervalo amplo | 200 | 4 IDs, em ordem: `50000000-0000-4000-8000-000000000006`, `50000000-0000-4000-8000-000000000001`, `50000000-0000-4000-8000-000000000003`, `50000000-0000-4000-8000-000000000005` |
| Unidade 2, intervalo amplo | 200 | 2 IDs, em ordem: `50000000-0000-4000-8000-000000000002`, `50000000-0000-4000-8000-000000000004` |
| Unidade 1, intervalo sem registros | 200 | `content=[]` |
| Sem `from` | 400 | `Required parameter 'from' is not present.` |
| Sem `to` | 400 | `Required parameter 'to' is not present.` |
| `from=2035-01-01T00:00:00Z`, `to=2020-01-01T00:00:00Z` | 200 | `content=[]`; a linha de base não rejeitava `from >= to` |
| Unidade inexistente, intervalo amplo | 404 | `TENANT_RESOURCE_NOT_FOUND` |

Os seis IDs retornados pertencem à mesma clínica seed e estão distribuídos
entre duas unidades. Portanto, o 400 da chamada sem `unitId` era um erro de
contrato de entrada, não ausência de appointments. As chamadas filtradas por
unidade já preservavam a ordenação por início e o isolamento do tenant.

## Cobertura automatizada pós-alteração

`AppointmentReadStatusIntegrationTest` reproduz o endpoint autenticado com
duas unidades visíveis da mesma clínica e outra clínica isolada. A execução
focada observada em 2026-11-08 concluiu com 12 testes, 0 falhas e 0 erros. Ela
verifica:

- busca sem `unitId` com registros das duas unidades visíveis;
- busca com `unitId` mantendo apenas a unidade solicitada;
- limite inicial inclusivo e limite final exclusivo;
- `from` e `to` obrigatórios, e `from >= to` respondendo 400;
- intervalo válido sem registros respondendo 200 com `content=[]`;
- unidade e appointment de outra clínica respondendo 404 com
  `TENANT_RESOURCE_NOT_FOUND`.

## Resultados observados após a correção

A versão atual foi iniciada com PostgreSQL 18 descartável, migrations `V001` a
`V010` e os seeds de desenvolvimento `R__001` a `R__009`. As duas unidades seed
estavam `ATIVO`. Foram repetidos os mesmos parâmetros e IDs técnicos da linha de
base.

| Caso | Status | Resultado técnico observado |
| --- | ---: | --- |
| Sem `unitId`, intervalo amplo | 200 | 6 IDs, em ordem: `50000000-0000-4000-8000-000000000006`, `50000000-0000-4000-8000-000000000001`, `50000000-0000-4000-8000-000000000002`, `50000000-0000-4000-8000-000000000003`, `50000000-0000-4000-8000-000000000004`, `50000000-0000-4000-8000-000000000005` |
| Unidade 1, intervalo amplo | 200 | Os mesmos 4 IDs da linha de base, na mesma ordem |
| Unidade 2, intervalo amplo | 200 | Os mesmos 2 IDs da linha de base, na mesma ordem |
| Sem `unitId`, intervalo sem registros | 200 | `content=[]` |
| Sem `from` | 400 | `Required parameter 'from' is not present.` |
| Sem `to` | 400 | `Required parameter 'to' is not present.` |
| `from=2035-01-01T00:00:00Z`, `to=2020-01-01T00:00:00Z` | 400 | `INVALID_APPOINTMENT_RANGE` |
| Unidade inexistente, intervalo amplo | 404 | `TENANT_RESOURCE_NOT_FOUND` |

## Comparação e verificação final

- a chamada sem `unitId` mudou de 400 para 200 e passou a agregar os seis
  appointments visíveis das duas unidades da clínica autenticada;
- as chamadas existentes com `unitId` preservaram status, quantidade, IDs e
  ordenação: 4 registros na unidade 1 e 2 na unidade 2;
- intervalo sem registros, parâmetros obrigatórios e unidade inexistente
  preservaram seus contratos;
- o intervalo invertido mudou de 200 vazio para 400 com
  `INVALID_APPOINTMENT_RANGE`, conforme a spec;
- a suíte `AppointmentReadStatusIntegrationTest` passou com 12 testes, 0
  falhas, 0 erros e 0 ignorados;
- `./mvnw clean verify` passou em 2026-11-08 com 34 testes, 0 falhas, 0 erros e
  0 ignorados, gerando `target/agendamento-smart-api-1.0.5.jar`.

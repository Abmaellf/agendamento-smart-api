# Design

## Context

Ver [proposal.md](proposal.md) e [delta de appointments](specs/appointments/spec.md). A inspeção estática em 20/11/2026 encontrou `Clock` injetado, `ClinicUnit.timeZone`, `@Version`, lock por appointment no início individual e lock de clínica na criação. `AGENDADO`/`ATENDENDO` contam como ativos; `FALTA` libera capacidade. O monitor atualiza condicionalmente somente `AGENDADO`.

O ciclo individual/monitor está na change ativa [implementar-transicoes-e-monitoramento-de-status-de-agendamento](../implementar-transicoes-e-monitoramento-de-status-de-agendamento/proposal.md). Seus artefatos relatam implementação e verificação direcionada, mas uma verificação ampla pendente; esta sessão não repetiu testes de runtime. A base ainda não recebeu seus requisitos. Este delta descreve a exceção coletiva em requisitos aditivos, sem tentar modificar requisitos que ainda pertencem à change anterior nem duplicar sua remoção de `Estado atual não possui transições HTTP`.

O service de criação já libera profissional compartilhado em serviço coletivo e bloqueia conflitos para `Atendimento individual`. Sua leitura estática orienta a reutilização da validação na reativação, sem ampliar a mudança para corrigir documentação antiga.

## Goals / Non-Goals

**Goals:** oferecer um comando de conjunto com autorização no servidor, presença explícita, ponto temporal único e resultado determinístico sobre os registros avaliados.

**Non-Goals:** reutilizar o comando individual como laço HTTP, criar um framework genérico de transições, mudar o monitor ou criar tabela de idempotência/auditoria.

## Decisions

### 1. Rota aditiva e DTO próprio

Adicionar `POST /api/appointments/start-current-hour` no controller de appointments, DTO de entrada estrito e resposta agregada própria. Somente `unitId` e `hourStart` são aceitos; campos adicionais são recusados apenas nesse contrato, sem mudar globalmente a desserialização dos endpoints legados.

O método de serviço recebe o principal e deriva `clinicId`, resolve a unidade pelo par unidade/clínica e aplica o papel. A política de `INATIVO`/`BLOQUEADA` acompanha o início individual observado; não usar permissões de criação como substituto. Ausência de vínculo válido nunca amplia o escopo.

Alternativa descartada: permitir `clinicId`, IDs de appointments ou `status` no corpo. O comando tem intenção e fronteira fixas.

### 2. Um início absoluto identifica a célula, o servidor determina sua validade

Interpretar `hourStart` como `Instant` e validar que, no `ZoneId` persistido da unidade, ele representa uma hora com minuto, segundo e fração zerados. Capturar `evaluatedAt = clock.instant()` após adquirir os locks necessários; truncar a representação zonada à hora para obter o início atual e derivar o fim. Comparar por instante, aceitando offset ou UTC equivalentes.

Usar operações zonadas para tratar meia-noite e transições de offset: uma hora repetida possui dois inícios absolutos distintos. Não montar instantes com o fuso do processo nem aceitar `hourEnd` do cliente. Validar novamente se a espera por lock atravessar a hora; após o ponto de avaliação, a transação pode concluir com a faixa já validada sem trocar os candidatos.

Alternativas descartadas: aceitar intervalo arbitrário ou receber só a unidade. A segunda faria um clique tardio agir silenciosamente sobre a próxima turma.

### 3. Lote por início e confirmação explícita de presença

Selecionar `clinic_id`, `unit_id` e `starts_at >= hourStart AND starts_at < hourEnd`, sem paginação nem filtros de serviço/profissional. Considerar estados `AGENDADO` e `FALTA` como candidatos; iniciar significa confirmar presença, inclusive às 08:30 para agendamento das 08:00. Não reutilizar o bloqueio de dez minutos de `updateStatus` no novo comando.

Registros iniciados em minutos futuros da mesma hora também são selecionados, conforme o conceito de célula solicitado. Preservar horário previsto, duração, profissional opcional e demais snapshots; `updatedAt` e versão registram a alteração existente, sem introduzir campo de início real.

O monitor continua responsável por falta automática para `AGENDADO` sem presença confirmada. Não chamar o reconciliador global ou por clínica dentro deste endpoint: ele poderia modificar registros de outras unidades/faixas e desfazer o limite do comando.

### 4. Atomicidade, capacidade e concorrência

Usar uma transação com rollback integral de falhas, distinta do `noRollbackFor` usado pelo erro de prazo individual. Adquirir primeiro o lock de clínica já usado por criação; selecionar registros da faixa com lock de escrita em ordem estável de ID e reavaliar estado após a aquisição. A lista selecionada define o conjunto da execução; registros criados posteriormente entram apenas numa nova chamada.

Revalidar as faltas a reativar contra o conjunto final proposto: ativos persistidos mais todos os candidatos a reativação. Reaproveitar, em helper restrito ao módulo quando necessário, os predicados vigentes de paciente/profissional individual/capacidade; excluir o próprio ID, preservar o escopo clínico dessas regras e não usar a validação de criação que exige data futura. Uma violação rejeita o lote inteiro com os códigos existentes, sem transições parciais.

Persistir mudanças de estado, `updatedAt` e versão somente após validações. O lock de clínica impede disputa com novas criações pela vaga liberada por falta; locks/versões por registro e a condição do monitor impedem perda de transição. Cobrir concorrência real com duas transações/PostgreSQL, incluindo o reconciliador de leitura: versões obsoletas não podem regravar `FALTA` sobre `ATENDENDO`. Lock timeout/deadlock deve resultar em rollback e erro, nunca sucesso parcial.

Alternativas descartadas: update massivo sem revalidar capacidade; sequência de transações por paciente; ou catch de erro por item com HTTP 200. Todas permitem resultados incompatíveis com o conjunto solicitado.

### 5. Repetição e resultado agregado

Idempotência é de efeito por estado, sem chave nova: `ATENDENDO` é ignorado, sem tocar versão/`updatedAt`. Nova chamada na mesma hora pode incluir um novo registro inserido após a anterior; a resposta não é armazenada/reproduzida. Cancelados e finalizados são ignorados. Se nenhum candidato existir, responder 200 com zero atualizações.

Contrato espelhado no design do frontend, com identificador sintético:

```http
POST /api/appointments/start-current-hour
Content-Type: application/json

{"unitId":"11111111-1111-4111-8111-111111111111","hourStart":"2026-11-20T08:00:00-04:00"}
```

```json
{
  "unitId": "11111111-1111-4111-8111-111111111111",
  "timeZone": "America/Cuiaba",
  "hourStart": "2026-11-20T12:00:00Z",
  "hourEnd": "2026-11-20T13:00:00Z",
  "evaluatedAt": "2026-11-20T12:30:00Z",
  "targetStatus": "ATENDENDO",
  "matchedCount": 6,
  "updatedCount": 3,
  "skippedCount": 3,
  "skippedByStatus": {"ATENDENDO": 1, "CANCELADO": 1, "FINALIZADO": 1}
}
```

Contar os ignorados segundo o estado observado sob lock; convertidos de `AGENDADO`/`FALTA` contam em `updatedCount`, não em `skippedByStatus`. As três chaves de ignorados são sempre presentes, inclusive com zero. Não devolver IDs de pacientes nem lista completa de appointments. O frontend refaz o GET existente após sucesso.

| Condição | Resposta |
| --- | --- |
| Corpo, UUID, campo extra ou instante inválido | 400, erro de validação; `INVALID_APPOINTMENT_HOUR` para formato/alinhamento temporal |
| Hora alinhada diferente da corrente | 409 `APPOINTMENT_HOUR_NOT_CURRENT` |
| Unidade inexistente/externa/não permitida | 404 `TENANT_RESOURCE_NOT_FOUND` |
| Clínica/unidade inativa | 403, códigos estruturais existentes |
| Fuso persistido inválido | 422 `UNIT_TIME_ZONE_INVALID` |
| Reativação produz conflito | 409, códigos existentes de paciente/profissional/capacidade |
| Falha técnica | Tratamento de erro existente, com rollback, sem contagens de sucesso |

Validar autorização antes de consultar candidatos ou produzir detalhes da unidade. Se houver vários conflitos, manter precedência paciente, profissional e capacidade, sem dados pessoais no erro. Autenticação inválida segue o mecanismo de segurança existente.

## Risks / Trade-offs

- [Reativar falta ocupa vaga liberada] → validar o conjunto final sob o lock da clínica usado pela criação; preservar snapshots, sem criar nova reserva.
- [Lock de clínica reduz paralelismo] → escopo restrito à hora; usar a estratégia já adotada e medir duração antes de propor otimizações ou índices/migrations.
- [Monitor/leitura concorrem com início] → testes transacionais e condições/versões; presença confirmada nunca é revertida pelo monitor.
- [Relógios diferentes ou virada sob lock] → validação autoritativa após espera, offset explícito e erro estável, sem troca silenciosa de faixa.
- [Implementação depende de transições ainda não promovidas] → aplicar/verificar a change de transições antes desta; manter este delta aditivo e registrar a pendência real sem inferir conclusão.

## Migration Plan

1. Garantir disponibilidade da implementação de transições/monitor, mantendo sua pendência de verificação visível; esta change não conclui nem arquiva aquela automaticamente.
2. Implementar DTOs, rota, autorização, seleção temporal, transação e resultado; não há migration ou dependência nova prevista.
3. Executar testes HTTP/concorrentes em PostgreSQL descartável, verificação Maven, revisão dos dois contratos e validação OpenSpec. Atualizar documentação entregue.
4. Disponibilizar a API antes do frontend. Um rollback pode retirar o endpoint e a ação visual, sem reverter status já confirmados nem alterar migrations executadas.

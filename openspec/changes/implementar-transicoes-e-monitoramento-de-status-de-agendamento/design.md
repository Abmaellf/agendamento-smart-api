# Design

## Context

Consulte `proposal.md` e o delta de `appointments` para o contrato. A API atual usa Spring Boot 3.4.5, JPA/Hibernate, PostgreSQL e Flyway; `AppointmentController` expõe somente criação e leitura, `AppointmentStatus` possui `AGENDADO`, `ATENDENDO`, `CANCELADO` e `FINALIZADO`, e a entidade usa `@Version` e `updatedAt`. Não há scheduler nem dependência de Actuator/Micrometer no projeto.

## Goals / Non-Goals

**Goals:**

- Expor transições seguras e idempotentemente rejeitáveis para iniciar e cancelar appointments.
- Persistir `FALTA` com base no instante absoluto `startsAt`, independente do fuso do processo ou de chamadas do frontend.
- Garantir que o monitor funcione com uma ou várias instâncias da API, sem reverter `ATENDENDO`.
- Tornar o resultado de listagem/detalhe consistente mesmo quando a rodada periódica estiver atrasada.
- Produzir observabilidade operacional suficiente para detectar atraso ou falha do monitor sem registrar dados pessoais.

**Non-Goals:**

- Criar workflow de conclusão, remarcação, recorrência, notificações ou tabela de eventos de auditoria.
- Adicionar um serviço externo de filas/cron, uma nova dependência de métricas ou alterar as permissões gerais da API.
- Aceitar `FALTA` como comando manual do cliente.

## Decisions

### 1. Manter `ATENDENDO` e adicionar somente `FALTA`

O enum público preservará `ATENDENDO`, compatível com o contrato observado e com as regras atuais de ocupação. A migration seguinte ampliará a constraint de status para `FALTA`; dados existentes não serão convertidos. O frontend deverá apresentar `ATENDENDO` como “Atendimento iniciado”.

Alternativa rejeitada: renomear imediatamente `ATENDENDO` para `EM_ATENDIMENTO` ou `EMATENDIMENTO`. Isso criaria uma migração incompatível e divergiria do estado persistido atual sem necessidade para o primeiro incremento.

### 2. Endpoint explícito com comando limitado

O controller receberá `PATCH /api/appointments/{id}/status` e um DTO com `AppointmentStatus status`, mas o service aceitará somente `ATENDENDO` e `CANCELADO` quando o estado atual for `AGENDADO`. `FALTA` será reservado ao reconciliador. O retorno será o mesmo `AppointmentResponse` usado em GET.

Cada transição manual carregará o appointment por clínica com lock pessimista, verificará a unidade do papel do usuário e avaliará o prazo antes de alterar o status. Recursos de outra clínica ou unidade continuarão indistinguíveis por `404/TENANT_RESOURCE_NOT_FOUND`.

Alternativa rejeitada: permitir `PATCH` genérico com qualquer valor do enum. Isso abriria reversões e comandos automáticos pelo cliente, contrariando a matriz de ciclo de vida.

### 3. Monitor híbrido: background mais reconciliação na leitura

O monitor será ativado por propriedade e executará a cada 60 segundos por padrão, com `initialDelay` configurável. Cada rodada seguirá o fluxo:

```text
agendado a cada intervalo
  -> calcular cutoff = clock.instant() - 10 minutos
  -> contar candidatos AGENDADO com startsAt <= cutoff
  -> UPDATE condicional: AGENDADO -> FALTA e version = version + 1
  -> registrar candidatos, atualizados, duração e erro
```

As consultas `findAll` e `findById` executarão uma reconciliação idempotente antes de montar a resposta. Assim, uma agenda aberta imediatamente após o limite não depende de esperar a próxima rodada. A atualização condicional usa `WHERE status = 'AGENDADO'` e não altera `ATENDENDO`, `CANCELADO` ou `FINALIZADO`.

Alternativa rejeitada: depender somente do scheduler. O atraso máximo configurado deixaria a resposta do GET incorreta por uma janela observável; um timer do frontend seria ainda menos confiável.

### 4. Concorrência entre início manual e falta automática

O comando manual usará lock pessimista por appointment. Se o lock for obtido antes do cutoff, `ATENDENDO` vence e o monitor não encontrará mais um registro `AGENDADO`. Se o monitor confirmar primeiro o cutoff, o comando verá `FALTA` e retornará conflito. O bulk update do monitor é condicional e pode ser executado por múltiplas instâncias sem duplicar a transição.

Quando uma tentativa manual chegar após o cutoff, o service persistirá `FALTA` e responderá `APPOINTMENT_NO_SHOW_DEADLINE` sem deixar a exceção desfazer essa reconciliação; isso será coberto por teste transacional.

### 5. Clock e configuração operacional

O service de status receberá `Clock` injetável, usando UTC como relógio do processo; `startsAt` já é um `Instant`, portanto a regra de dez minutos não depende de `timeZone`. O intervalo será configurável por `appointments.status-monitor.fixed-delay-ms`, com habilitação e atraso inicial também configuráveis, mantendo defaults seguros para execução local.

Alternativa rejeitada: chamar `Instant.now()` diretamente em cada método. Isso dificulta testes de fronteira e pode produzir resultados inconsistentes entre a transição e a reconciliação.

### 6. Observabilidade por logs operacionais

Sem introduzir Actuator/Micrometer nesta change, o monitor emitirá logs estruturados em nível `INFO` para rodadas concluídas e `WARN/ERROR` para falhas, com `runId`, `startedAt`, `durationMs`, `candidates`, `updated` e `errorType`. Nenhum log conterá paciente, e-mail, token, payload ou appointment ID. A documentação operacional deverá indicar que alertas podem contar falhas e ausência de rodadas dentro de duas janelas configuradas.

## Risks / Trade-offs

- [Bulk update concorrente com transição manual] → usar condição de status, lock pessimista no comando manual e testes com duas transações para validar o vencedor.
- [Scheduler pausado ou processo reiniciado] → reconciliar nas leituras e executar nova rodada após o startup; o estado não dependerá da memória do processo.
- [Grande volume de appointments vencidos] → consultar/atualizar em lotes ou por janela limitada, registrar contagens e manter índice por status/início se a medição indicar necessidade.
- [Exceção após persistir FALTA] → separar a reconciliação do erro de domínio ou configurar transação sem rollback para o erro específico, com teste que confirme a persistência.
- [Logs insuficientes para alerta externo] → padronizar campos fixos e documentar a regra de alerta por falha/atraso; a instrumentação de métricas fica para uma change de observabilidade própria.

## Migration Plan

1. Criar migration Flyway para ampliar a constraint `chk_appointment_status` com `FALTA` e, se necessário, o índice de busca por `status`/`starts_at`.
2. Adicionar DTO, endpoint, validação de transição, lock por clínica/unidade e atualização de `updatedAt`/`version`.
3. Adicionar reconciliador de leitura e monitor agendado com propriedades configuráveis e logs operacionais.
4. Cobrir transições, autorização, prazo, concorrência, leitura reconciliada, scheduler e migration com testes; executar `./mvnw clean verify` e a validação PostgreSQL descartável.
5. Em rollback, desligar o monitor e reverter o deploy da API; não editar migrations já executadas nem remover registros de appointments. A remoção do endpoint e de `FALTA` exige change própria de compatibilidade.

## Open Questions

Nenhuma. O intervalo padrão, a fonte temporal, o contrato do endpoint e a estratégia híbrida de monitoramento estão definidos nesta change.

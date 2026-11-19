# Proposal

## Why

A API hoje cria e consulta agendamentos, mas não oferece a transição necessária para iniciar ou cancelar um atendimento e não marca automaticamente a ausência do paciente. Isso impede que a agenda web mantenha o ciclo de vida do agendamento persistido e consistente após recarregamentos, múltiplas instâncias ou consultas tardias.

## What Changes

- Adicionar `FALTA` ao conjunto de estados persistidos sem remover os estados existentes.
- Expor `PATCH /api/appointments/{id}/status` para as transições manuais permitidas: `AGENDADO` → `ATENDENDO` e `AGENDADO` → `CANCELADO`.
- Restringir a atualização à clínica autenticada e à unidade permitida pelo papel do usuário, preservando `404` para recursos de outra clínica e as regras de leitura da unidade.
- Rejeitar transições inválidas, repetidas ou feitas depois do limite de ausência com códigos e HTTP determinísticos; o endpoint não aceitará `FALTA` como mudança manual do cliente.
- Marcar `AGENDADO` como `FALTA` quando `startsAt + 10 minutos` for atingido sem que o atendimento tenha sido iniciado, usando o instante absoluto persistido.
- Monitorar a regra automática em background com execução periódica idempotente e reconciliação na leitura da agenda/detalhe para reduzir a janela entre o vencimento e a resposta observada.
- Registrar cada execução do monitor com quantidade candidata, quantidade atualizada, duração e erro, sem incluir dados pessoais ou tokens nos logs.

Fora de escopo: conclusão (`CONCLUIDO`/`FINALIZADO`), remarcação, recorrência, edição de dados do agendamento, observações clínicas, notificações ao paciente, criação de uma tabela de auditoria de eventos e alteração de permissões globais.

## Capabilities

### New Capabilities

Nenhuma. As transições fazem parte da capacidade existente de appointments.

### Modified Capabilities

- `appointments`: adicionar transições autorizadas, o estado automático `FALTA` e o monitoramento idempotente do prazo de ausência.

## Impact

- Afeta controller, DTO, service, repository, enum e migration de appointments, além dos testes HTTP/transacionais.
- Introduz o contrato HTTP `PATCH /api/appointments/{id}/status` com corpo `{ "status": "ATENDENDO" | "CANCELADO" }` e resposta `AppointmentResponse` atualizado.
- Altera a constraint de status da tabela `APPOINTMENT` para aceitar `FALTA`; registros existentes permanecem inalterados.
- Adiciona configuração de intervalo do monitor e uma execução agendada no processo da API, sem dependência de chamadas do frontend para funcionar.
- O frontend deverá consumir `ATENDENDO` como “Atendimento iniciado” e `FALTA` como “Falta”; a change correspondente da agenda web precisa alinhar seus tipos a esse contrato.

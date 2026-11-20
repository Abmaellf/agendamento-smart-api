# Proposal

## Why

A API já possui início individual por `PATCH /api/appointments/{id}/status`, mas não oferece um comando para iniciar os agendamentos da hora corrente em conjunto. O botão `Iniciar todos` da agenda precisa de uma operação única que selecione e atualize os registros no servidor, preservando isolamento por clínica/unidade, prazo de falta e concorrência.

## What Changes

- Adicionar `POST /api/appointments/start-current-hour`, autenticado, com corpo `{ unitId, hourStart }` e resposta agregada das atualizações e dos registros ignorados.
- Derivar a clínica do usuário autenticado, exigir uma unidade explícita pertencente à clínica e aplicar a restrição de unidade do `BASIC`; `ADMIN` pode operar uma unidade autorizada da própria clínica por chamada.
- Validar a hora corrente pelo relógio do servidor e pelo fuso persistido da unidade. `hourStart` identifica a célula acionada e não autoriza datas/horas arbitrárias.
- Selecionar todos os appointments cujo `startsAt` pertença a `[hourStart,hourEnd)`, sem depender de IDs, filtros de serviço/profissional ou paginação do cliente.
- Tratar o comando como confirmação explícita de presença, convertendo `AGENDADO` e `FALTA` para o status existente `ATENDENDO`, inclusive após dez minutos, preservando já iniciados, cancelados e finalizados.
- Revalidar conflitos e capacidade ao reativar faltas, pois esses registros deixaram de contar como ativos; rejeitar o lote inteiro se a reativação produzir conflito.
- Produzir resultado com contagens, garantir transação sem sucesso técnico parcial e repetição sem transição duplicada, além de concorrência segura com o monitor e o comando individual.

Estado observado: `AppointmentService.updateStatus`, `AppointmentRepository` e o monitor implementam o ciclo individual e o prazo de falta. A spec base ainda contém uma restrição antiga de ausência de transições; sua remoção pertence à change ativa `implementar-transicoes-e-monitoramento-de-status-de-agendamento`, da qual esta proposta depende. Não há evidência de execução integrada nova nesta etapa de planejamento.

Decisão do usuário: iniciar atendimento confirma que a pessoa está no local. Portanto, às 08:30 o novo comando pode converter uma falta das 08:00 em `ATENDENDO`. Essa exceção é específica do comando coletivo; o monitor e o contrato individual permanecem com suas regras atuais.

Fora de escopo: código nesta etapa, novos estados/migrations, alteração da execução automática do prazo de falta, reabertura de cancelados/finalizados, edição do início individual, novos filtros/catálogos, seleção de várias unidades, eventos de auditoria novos, notificações e refatoração de regras legadas.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `appointments`: comando de início coletivo da hora atual, com escopo autenticado, seleção temporal, resultado agregado e garantias transacionais.

## Impact

- Implementação futura em controller/DTOs/service/repository de appointments, usando `Clock`, `ClinicUnit.timeZone`, enum e controle de versão existentes.
- Adiciona contrato HTTP, sem mudar os endpoints de criação, leitura e status individual nem exigir alteração de schema ou dependência.
- Exige testes HTTP, autorização, isolamento, fronteiras temporais e concorrência em PostgreSQL descartável, além de atualizar `docs/API_APPOINTMENTS.md` e READMEs afetados quando implementado.
- Consumidor: [proposta do frontend irmão](../../../../../agendamento-smart-web/agendamentos-smart/openspec/changes/iniciar-agendamentos-da-hora-atual-em-lote/proposal.md).
- Dependência de implementação: [transições e monitor](../implementar-transicoes-e-monitoramento-de-status-de-agendamento/proposal.md), preservando sua pendência real de verificação. Os requisitos desta change são aditivos e podem ser validados/arquivados sem tentar modificar requisitos ainda não promovidos.

# Tasks

## 1. Estado persistido e contratos

- [x] 1.1 Adicionar uma migration Flyway nova que permita `FALTA` na constraint `chk_appointment_status`, preserve os registros existentes e crie índice por status/início somente se a consulta de candidatos exigir; verificar em PostgreSQL descartável que Flyway aplica sem alterar appointments históricos.
- [x] 1.2 Atualizar `AppointmentStatus` e criar o DTO de comando de status mantendo `ATENDENDO` como valor público e restringindo o corpo HTTP a `ATENDENDO`/`CANCELADO`; verificar com testes de serialização/validação que `FALTA` não é aceito como comando manual.
- [x] 1.3 Atualizar a documentação do módulo e o contrato HTTP de appointments com os estados, códigos de erro e resposta do `PATCH`; verificar que os exemplos não exponham credenciais ou dados pessoais.

## 2. Transições manuais autorizadas

- [x] 2.1 Adicionar consultas de repository para carregar appointment por clínica com lock pessimista e para atualizar candidatos automáticos somente quando o status ainda for `AGENDADO`; verificar com teste de repository que clínica, status e condição temporal fazem parte da operação.
- [x] 2.2 Implementar no service `ATENDENDO` e `CANCELADO` somente a partir de `AGENDADO`, aplicando unidade autorizada por papel, prazo de dez minutos, `updatedAt`/`version` e os códigos `TENANT_RESOURCE_NOT_FOUND`, conflito de transição e `APPOINTMENT_NO_SHOW_DEADLINE`; verificar com testes transacionais de autorização, ciclo de vida e corrida de limite.
- [x] 2.3 Expor `PATCH /api/appointments/{id}/status` com HTTP 200 e `AppointmentResponse` atualizado, mantendo o contrato de criação e leitura; verificar com testes HTTP de início, cancelamento, payload inválido, recurso de outra clínica/unidade e status não permitido.

## 3. Monitor automático e reconciliação

- [x] 3.1 Implementar o reconciliador que calcula `cutoff = clock.instant() - 10 minutos` e transforma somente `AGENDADO` vencido em `FALTA`, incluindo persistência da falta antes de retornar o erro de prazo; verificar com relógio controlado os limites antes, exatamente em e depois de dez minutos.
- [x] 3.2 Integrar a reconciliação às consultas de listagem/detalhe e criar o monitor periódico configurável, habilitado por padrão com intervalo de 60 segundos e atraso inicial configurável; verificar que GET retorna `FALTA` mesmo sem rodada anterior e que reinício do processo não perde candidatos.
- [x] 3.3 Garantir idempotência em múltiplas instâncias e concorrência com início manual usando atualização condicional/lock, sem alterar `ATENDENDO`, `CANCELADO` ou `FINALIZADO`; verificar com testes concorrentes e duas execuções do monitor sobre os mesmos registros.
- [x] 3.4 Registrar logs operacionais de cada rodada com `runId`, duração, candidatos, atualizados e tipo de erro, sem dados pessoais ou tokens; verificar com teste de captura de logs uma rodada bem-sucedida e uma falha recuperável.

## 4. Verificação integrada

- [x] 4.1 Executar os testes direcionados de appointments e migration em PostgreSQL descartável, cobrindo autorização por clínica/unidade, transições, limite, leitura reconciliada, scheduler e concorrência; registrar a evidência observada.
- [ ] 4.2 Executar `./mvnw clean verify` e `openspec validate implementar-transicoes-e-monitoramento-de-status-de-agendamento --strict`, registrando separadamente qualquer falha preexistente ou dependência de ambiente.
- [x] 4.3 Conferir o contrato consumível pelo frontend, incluindo `ATENDENDO` como “Atendimento iniciado”, `FALTA` como “Falta” e `PATCH /api/appointments/{id}/status`; verificar que a change da API e a change `adicionar-detalhes-do-agendamento-paciente` não possuem nomes de status divergentes.

## Workflow follow-up

- Atualizar os checkboxes somente com evidência real de implementação e testes.
- Arquivar a change da API somente após validação estrita, migration verificada e revisão do contrato pelo consumidor frontend.
- A implementação da API foi aplicada nesta execução; aguardar confirmação explícita antes de aplicar código da change frontend `adicionar-detalhes-do-agendamento-paciente`.

## Evidência da execução

- PostgreSQL descartável e suíte direcionada de appointments/monitor: aprovados.
- `openspec validate implementar-transicoes-e-monitoramento-de-status-de-agendamento --strict`: aprovado.
- `./mvnw clean verify`: pendente por uma falha preexistente em `ClinicStructureEndToEndIntegrationTest.runsTwoClinicWorkflowWithInitialTeamLinksAndIsolatedAppointmentSearches`, que espera HTTP 200 no cadastro de paciente, enquanto o endpoint atual responde HTTP 201; o fluxo não foi alterado por esta change.

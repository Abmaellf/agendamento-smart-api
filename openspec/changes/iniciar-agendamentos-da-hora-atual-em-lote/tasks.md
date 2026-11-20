# Tasks

## 1. Contrato HTTP e autorização

- [ ] 1.1 Adicionar DTO de entrada estrito, resposta agregada e rota autenticada `POST /api/appointments/start-current-hour`; verificar testes HTTP de campos obrigatórios, UUID/instante, rejeição de campos extras e formato das contagens, sem alterar os endpoints legados.
- [ ] 1.2 Resolver clínica pelo principal e unidade pela clínica, aplicando `ADMIN`/`BASIC` e os status estruturais definidos; verificar duas clínicas, duas unidades, unidade inexistente, falta de autenticação, `INATIVO` e `BLOQUEADA`, sem consulta/atualização de lote externo.
- [x] 1.3 Atualizar `docs/API_APPOINTMENTS.md` e READMEs de controller/DTOs com payload, resposta, erros, escopo e exceção de presença coletiva; conferir os exemplos sintéticos contra o design do frontend.

## 2. Faixa temporal e seleção

- [ ] 2.1 Calcular a faixa no fuso persistido da unidade usando `Clock`, validar alinhamento e igualdade com a hora corrente e distinguir 400/409/422; verificar com relógio controlado 07:00, 08:30, fronteira 09:00, meia-noite, offsets equivalentes, hora repetida e fuso inválido.
- [ ] 2.2 Adicionar seleção por clínica/unidade/início semiaberto com lock e ordem estável; verificar em PostgreSQL descartável inclusão de 08:00/08:30/08:59, exclusão de 07:30/09:00, duração atravessando a faixa e escopo de contagens sem paginação.
- [x] 2.3 Revalidar o instante após espera pelos locks e fixar o conjunto avaliado; verificar que uma espera até a próxima hora responde `APPOINTMENT_HOUR_NOT_CURRENT` sem alterar qualquer faixa e documentar esse ponto temporal no README do service.

## 3. Transação de presença e consistência

- [ ] 3.1 Implementar `AGENDADO`/`FALTA` → `ATENDENDO` sem bloqueio de dez minutos nesse comando, ignorando estados preservados; verificar antes/no/depois do limite, profissional nulo, snapshots intactos, `updatedAt`/versão somente nos alterados e ausência de reconciliação fora do lote.
- [ ] 3.2 Revalidar reativação de faltas contra ativos e candidatos usando as regras vigentes de paciente, profissional individual e capacidade, sob o lock de clínica compatível com criação; verificar vaga reutilizada, conflitos entre candidatos, exclusão do próprio ID, turma coletiva válida e rollback integral em conflito.
- [x] 3.3 Produzir contagens sob lock, resposta vazia e repetição sem efeito adicional; verificar `matchedCount = updatedCount + skippedCount`, soma por estado, chaves zeradas, segunda chamada sem incremento de versão e ausência de dados pessoais na resposta/erros.
- [ ] 3.4 Verificar e garantir concorrência com duas chamadas coletivas, início/cancelamento individual, monitor, reconciliação de leitura e criação disputando vaga em PostgreSQL; testar ambos os vencedores e que `ATENDENDO` confirmado nunca volta a `FALTA`.
- [ ] 3.5 Garantir rollback em falha de persistência/lock sem sucesso parcial; verificar falha injetada durante gravação e registrar nos READMEs de service/modelo a atomicidade e a diferença entre confirmação coletiva e comando individual.

## 4. Verificação integrada

- [ ] 4.1 Executar a suíte direcionada nova e a regressão `AppointmentReadStatusIntegrationTest`, `AppointmentStatusMonitorTest` e testes de criação com PostgreSQL descartável; registrar resultados reais e confirmar que a política individual não mudou.
- [ ] 4.2 Executar `./mvnw clean verify` e `openspec validate iniciar-agendamentos-da-hora-atual-em-lote --strict`; registrar separadamente qualquer falha preexistente, incluindo a pendência já documentada na change de transições/monitor se ainda ocorrer.
- [ ] 4.3 Conferir integração com o frontend usando o mesmo contrato e dados sintéticos de dois tenants/unidades; verificar retorno, repetição, confirmação de falta, faixa expirada e mensagens de conflito, além de revisar cobertura dos requisitos e documentação.

## Workflow follow-up

- A aplicação foi iniciada: tarefas marcadas refletem implementação e evidência
  executadas; as demais continuam pendentes até os testes HTTP/PostgreSQL e de
  concorrência previstos.
- Aplicar e verificar a change `implementar-transicoes-e-monitoramento-de-status-de-agendamento` antes deste código. Este delta é aditivo e não altera os requisitos ainda não promovidos daquela change; não encerrar a dependência por inferência nem sobrescrever suas pendências.
- Disponibilizar a API antes do frontend e seguir o protocolo de confirmação para deploy ou ações irreversíveis.
- Arquivar somente após conclusão efetiva, documentação atualizada e validação estrita.

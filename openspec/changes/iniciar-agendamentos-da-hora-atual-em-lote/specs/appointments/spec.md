# Spec Delta

## ADDED Requirements

### Requirement: A API oferece comando coletivo de início na hora corrente

`POST /api/appointments/start-current-hour` MUST exigir autenticação e corpo com `unitId` UUID e `hourStart` ISO 8601 com offset ou `Z`. MUST rejeitar corpo inválido ou campos de seleção não previstos, incluindo clínica, status e lista de IDs. O servidor MUST determinar a seleção e o status de destino.

#### Scenario: Comando válido
- **WHEN** um usuário autorizado solicitar a hora corrente de uma unidade permitida
- **THEN** a API MUST processar o lote dessa unidade e responder HTTP 200 com o resultado agregado

#### Scenario: Corpo inválido ou tentativa de escolher status
- **WHEN** faltar campo obrigatório, o UUID/instante for inválido ou o cliente enviar `tenantId`, `clinicId`, `status`, `appointmentIds` ou `hourEnd`
- **THEN** a API MUST responder HTTP 400 e MUST não alterar agendamentos

#### Scenario: Sem autenticação
- **WHEN** a chamada não possuir sessão/token válido
- **THEN** a API MUST recusar a operação pelo mecanismo de autenticação existente e MUST não consultar nem alterar o lote

### Requirement: O lote é isolado pela clínica autenticada e por uma unidade autorizada

A clínica MUST vir exclusivamente da identidade autenticada. A unidade MUST pertencer à clínica e ser permitida ao usuário: `BASIC` somente na unidade associada; `ADMIN` em uma unidade da própria clínica por chamada. Seleção, contagens e alterações MUST permanecer nesse escopo, inclusive para faltas.

#### Scenario: BASIC na própria unidade
- **WHEN** `BASIC` solicitar sua unidade e houver agendamentos na mesma hora em outras unidades/clínicas
- **THEN** somente os registros da unidade autorizada MUST integrar a resposta e a atualização

#### Scenario: Unidade externa ou não autorizada
- **WHEN** a unidade não existir, pertencer a outra clínica ou não for a unidade autorizada ao `BASIC`
- **THEN** a API MUST responder HTTP 404 com `TENANT_RESOURCE_NOT_FOUND`, sem expor existência, contagens ou registros externos

#### Scenario: ADMIN escolhe uma unidade da clínica
- **WHEN** `ADMIN` solicitar uma unidade pertencente à própria clínica
- **THEN** a API MUST processar somente essa unidade e MUST não ampliar a operação para a clínica inteira

### Requirement: O início coletivo preserva as restrições de status da estrutura

O comando MUST aplicar a mesma política estrutural do início individual existente: clínica ou unidade `INATIVO` impede a operação; `ATIVO` e `BLOQUEADA` permitem processar agendamentos existentes, sujeitos às demais validações. Essa permissão MUST não autorizar criação de novos agendamentos.

#### Scenario: Estrutura inativa
- **WHEN** a clínica ou a unidade autorizada estiver `INATIVO`
- **THEN** a API MUST responder HTTP 403 com `CLINIC_APPOINTMENTS_READ_DISABLED` ou `UNIT_APPOINTMENTS_READ_DISABLED`, respectivamente, e MUST não alterar registros

#### Scenario: Estrutura bloqueada com agendamento existente
- **WHEN** clínica/unidade estiver `BLOQUEADA`, sem componente `INATIVO`, e o usuário estiver autorizado
- **THEN** o início coletivo de registros existentes MUST seguir a política do início individual, sem habilitar criação nem alterar o status da estrutura

### Requirement: O servidor valida a hora corrente no fuso persistido da unidade

A API MUST calcular hoje e a hora corrente pelo próprio relógio no fuso IANA da unidade. `hourStart` MUST representar o início absoluto dessa hora e `hourEnd` MUST ser derivado pelo servidor. Hora passada, futura ou vencida durante a espera pelo processamento MUST ser recusada, sem transferir a ação para outra faixa.

#### Scenario: Hora inteira e minutos intermediários
- **WHEN** o relógio indicar 07:00 ou 08:30 no fuso da unidade
- **THEN** a API MUST aceitar, respectivamente, a hora `[07:00,08:00)` ou `[08:00,09:00)` do mesmo dia local

#### Scenario: Instante não representa início de hora
- **WHEN** `hourStart` corresponder a 08:30, segundos/fração diferentes de zero ou data sem offset
- **THEN** a API MUST responder HTTP 400 com `INVALID_APPOINTMENT_HOUR` e MUST não alterar registros

#### Scenario: Outra hora ou dia
- **WHEN** `hourStart` estiver alinhado à hora, mas identificar hora passada, futura ou outra data
- **THEN** a API MUST responder HTTP 409 com `APPOINTMENT_HOUR_NOT_CURRENT` e MUST não alterar registros

#### Scenario: Virada durante espera concorrente
- **WHEN** uma requisição das 08:00 aguardar até a hora local passar a 09:00 antes da avaliação transacional do lote
- **THEN** a API MUST responder `APPOINTMENT_HOUR_NOT_CURRENT`, sem processar 08:00 nem substituir a seleção por 09:00

#### Scenario: Fuso do cliente ou processo diferente
- **WHEN** o cliente/processo estiver em outro fuso, inclusive próximo à meia-noite
- **THEN** a validação MUST usar a data e hora da unidade e os instantes absolutos, aceitando representações equivalentes em UTC ou offset

#### Scenario: Fuso IANA inválido na unidade
- **WHEN** o fuso persistido não puder ser resolvido
- **THEN** a API MUST responder HTTP 422 com `UNIT_TIME_ZONE_INVALID` sem recorrer ao fuso do processo nem atualizar registros

### Requirement: O lote inclui todos os inícios dentro da faixa semiaberta

A seleção MUST incluir os registros da clínica/unidade autorizadas cujo `startsAt` esteja em `[hourStart,hourEnd)`, independentemente de duração, serviço, profissional, origem manual/pacote ou paginação da tela. Registros fora da faixa MUST não ser alterados por esse comando, mesmo quando sua duração atravessar a hora corrente.

#### Scenario: Limites e minutos da mesma hora
- **WHEN** a faixa for `[08:00,09:00)` e existirem inícios às 07:30, 08:00, 08:30, 08:59 e 09:00
- **THEN** somente 08:00, 08:30 e 08:59 MUST compor o lote, inclusive quando o minuto de início ainda for futuro dentro da hora

#### Scenario: Agendamento longo iniciado antes da faixa
- **WHEN** um agendamento iniciado às 07:30 durar até 09:30 e a faixa solicitada for `[08:00,09:00)`
- **THEN** o comando MUST preservar esse registro, independentemente da sobreposição visual na agenda

### Requirement: O comando coletivo confirma presença mesmo após falta

Para os registros selecionados, `AGENDADO` e `FALTA` MUST passar para `ATENDENDO`, independentemente do prazo de dez minutos. `ATENDENDO`, `CANCELADO` e `FINALIZADO` MUST ser preservados e contabilizados como ignorados. A atualização MUST preservar demais dados e atualizar `updatedAt` e versão somente dos registros efetivamente alterados.

#### Scenario: Presença confirmada às 08:30
- **WHEN** a faixa corrente contiver agendamentos das 08:00 em `AGENDADO` vencido e `FALTA`
- **THEN** ambos MUST passar para `ATENDENDO` quando o lote for válido, sem erro `APPOINTMENT_NO_SHOW_DEADLINE`

#### Scenario: Cancelados e finalizados misturados
- **WHEN** o lote também contiver `ATENDENDO`, `CANCELADO` e `FINALIZADO`
- **THEN** esses registros MUST manter status, `updatedAt` e versão e MUST não impedir o início dos elegíveis

#### Scenario: Dados de negócio preservados
- **WHEN** o lote iniciar atendimentos, inclusive registros sem profissional atribuído
- **THEN** MUST preservar paciente, clínica, unidade, serviço, profissional opcional, horário, duração, preço e autoria de criação, sem criar novo agendamento

### Requirement: Reativar faltas não cria conflitos nem excede capacidade

Ao converter faltas em atendimentos ativos, a API MUST validar o conjunto resultante com as regras vigentes de sobreposição do paciente, exclusividade do profissional para `Atendimento individual` e capacidade do serviço. A validação MUST considerar registros ativos e todas as faltas reativadas juntas, sem contar o próprio registro duas vezes. Um conflito MUST rejeitar todo o lote.

#### Scenario: Vaga liberada por falta já foi ocupada
- **WHEN** reativar uma falta fizer a ocupação ultrapassar a capacidade do serviço
- **THEN** a API MUST responder HTTP 409 com `SERVICE_CAPACITY_EXCEEDED` e MUST não confirmar nenhuma transição do lote

#### Scenario: Reativação conflita com paciente ou profissional individual
- **WHEN** a falta a reativar sobrepuser outro atendimento ativo do paciente ou do profissional de `Atendimento individual`
- **THEN** a API MUST responder HTTP 409 com `PATIENT_APPOINTMENT_CONFLICT` ou `PROFESSIONAL_APPOINTMENT_CONFLICT`, sem alterar o lote

#### Scenario: Turma coletiva compatível
- **WHEN** vários pacientes distintos compartilharem profissional em serviço coletivo com capacidade suficiente e sem conflito do paciente
- **THEN** a API MUST permitir o lote, mantendo a regra existente de atendimento em grupo

### Requirement: O resultado do comando informa contagens consistentes

A resposta HTTP 200 MUST conter `unitId`, `timeZone`, `hourStart`, `hourEnd`, `evaluatedAt`, `targetStatus: ATENDENDO`, `matchedCount`, `updatedCount`, `skippedCount` e `skippedByStatus` com `ATENDENDO`, `CANCELADO` e `FINALIZADO`. As contagens MUST ser não negativas, com `matchedCount = updatedCount + skippedCount` e soma dos ignorados igual a `skippedCount`, sem dados pessoais.

#### Scenario: Resultado misto
- **WHEN** houver dois `AGENDADO`, um `FALTA`, um `ATENDENDO`, um `CANCELADO` e um `FINALIZADO`, sem conflito
- **THEN** a resposta MUST informar `matchedCount=6`, `updatedCount=3`, `skippedCount=3` e um ignorado em cada estado preservado

#### Scenario: Faixa vazia
- **WHEN** não houver agendamentos no escopo autorizado
- **THEN** a API MUST responder HTTP 200 com todos os contadores zerados e o intervalo processado

### Requirement: O início coletivo é atômico e seguro sob repetição e concorrência

O lote MUST confirmar integralmente as transições elegíveis ou reverter suas alterações em falha de domínio/persistência. Chamadas repetidas MUST não alterar novamente registros já em `ATENDENDO`. Concorrência com início individual, criação e monitor MUST preservar estados válidos, isolamento e capacidade; nenhuma execução posterior do monitor MUST reverter presença confirmada.

#### Scenario: Repetição após sucesso
- **WHEN** a chamada for repetida na mesma hora sem novos registros elegíveis
- **THEN** a API MUST responder HTTP 200 com `updatedCount=0`, contabilizando os já iniciados como ignorados sem atualizar suas versões

#### Scenario: Monitor marca falta antes do comando
- **WHEN** o monitor confirmar `FALTA` antes da avaliação do lote
- **THEN** a confirmação coletiva de presença MUST poder iniciar esse registro e o monitor MUST não revertê-lo depois

#### Scenario: Comando confirma presença antes do monitor
- **WHEN** o lote confirmar `ATENDENDO` antes de o monitor atualizar o mesmo registro
- **THEN** o monitor MUST preservar `ATENDENDO` e nenhuma transição MUST ser perdida

#### Scenario: Dois comandos ou cancelamento individual concorrentes
- **WHEN** dois comandos coletivos ou um comando individual disputarem os mesmos registros
- **THEN** cada transição MUST ocorrer uma única vez; cancelamento confirmado primeiro MUST ser preservado, e início confirmado primeiro MUST continuar impedindo cancelamento individual

#### Scenario: Falha durante a gravação
- **WHEN** ocorrer falha de persistência antes da confirmação da transação
- **THEN** todas as alterações desse comando MUST ser revertidas e a API MUST não responder sucesso ou contagens parciais

#### Scenario: Criação concorrente utiliza capacidade liberada
- **WHEN** uma criação e a reativação de uma falta disputarem a última vaga
- **THEN** a decisão MUST ser serializada e somente a operação compatível com a capacidade após a primeira confirmação MUST ser aceita

### Requirement: O comando coletivo é uma exceção restrita ao ciclo individual

A API MUST manter inalteradas as transições do endpoint individual. Somente o comando coletivo da hora corrente MUST aceitar `FALTA` para `ATENDENDO` como confirmação explícita de presença, sempre com o isolamento da clínica e da unidade autenticadas e as demais regras do lote.

#### Scenario: Endpoint individual permanece restrito
- **WHEN** um cliente tentar iniciar individualmente um appointment em `FALTA`
- **THEN** a API MUST preservar a regra do endpoint individual e MUST não aplicar a exceção coletiva a esse comando

#### Scenario: Exceção coletiva confirma presença
- **WHEN** o comando coletivo autorizado encontrar `FALTA` na hora corrente
- **THEN** MUST permitir `ATENDENDO` conforme as regras do lote, sem ampliar as transições permitidas pelo endpoint individual

### Requirement: O comando coletivo confirma presença após a falta automática

O comando coletivo da hora corrente MUST poder confirmar presença após `startsAt + 10 minutos` para registros em `AGENDADO` ou `FALTA`. Essa confirmação MUST preservar a regra automática de falta para registros que não forem iniciados e MUST impedir que o monitor reverta um registro já confirmado como `ATENDENDO`.

#### Scenario: Confirmação coletiva após o limite
- **WHEN** um lote válido confirmar presença após dez minutos para `AGENDADO` ou `FALTA` da hora corrente
- **THEN** a API MUST persistir `ATENDENDO` e MUST não aplicar o bloqueio de prazo do comando individual a esse lote

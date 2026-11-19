# Spec Delta

## ADDED Requirements

### Requirement: A API permite transições manuais de status do appointment

`PATCH /api/appointments/{id}/status` MUST receber `status` como `ATENDENDO` ou `CANCELADO` e MUST responder o appointment atualizado quando a transição for válida.

#### Scenario: Iniciar atendimento

- **WHEN** usuário autorizado envia `ATENDENDO` para appointment em `AGENDADO` antes do limite de falta
- **THEN** a API MUST persistir `ATENDENDO`, atualizar `updatedAt` e responder HTTP 200 com o appointment atualizado

#### Scenario: Cancelar atendimento aguardando

- **WHEN** usuário autorizado envia `CANCELADO` para appointment em `AGENDADO` antes do limite de falta
- **THEN** a API MUST persistir `CANCELADO`, atualizar `updatedAt` e responder HTTP 200 com o appointment atualizado

#### Scenario: Status não permitido pelo cliente

- **WHEN** o corpo omitir `status` ou solicitar `FALTA`, `FINALIZADO`, `CONCLUIDO` ou outro valor não previsto
- **THEN** a API MUST rejeitar a solicitação com erro de validação e MUST preservar o status persistido

### Requirement: As transições respeitam clínica, unidade e ciclo de vida

A API MUST aplicar o isolamento da clínica e da unidade do usuário e MUST aceitar somente transições originadas em `AGENDADO`, sem permitir reversão ou cancelamento após o atendimento iniciar.

#### Scenario: Appointment de outra clínica

- **WHEN** usuário envia uma transição para ID pertencente a outra clínica
- **THEN** a API MUST responder `TENANT_RESOURCE_NOT_FOUND` com HTTP 404 sem revelar o appointment

#### Scenario: Usuário restrito a outra unidade

- **WHEN** usuário não administrador envia uma transição para appointment de unidade diferente da sua unidade autorizada
- **THEN** a API MUST responder `TENANT_RESOURCE_NOT_FOUND` com HTTP 404 sem alterar o appointment

#### Scenario: Transição de estado não aguardando

- **WHEN** o appointment estiver em `ATENDENDO`, `CANCELADO`, `FALTA` ou `FINALIZADO`
- **THEN** a API MUST responder conflito de transição e MUST preservar o estado atual

### Requirement: O prazo de falta impede início ou cancelamento tardios

Depois de `startsAt + 10 minutos`, um appointment ainda `AGENDADO` MUST ser tratado como falta e MUST deixar de aceitar `ATENDENDO` ou `CANCELADO`.

#### Scenario: Início antes do limite

- **WHEN** `ATENDENDO` for confirmado antes de `startsAt + 10 minutos`
- **THEN** o appointment MUST permanecer `ATENDENDO` após o limite e não MUST ser convertido para `FALTA`

#### Scenario: Início após o limite

- **WHEN** usuário tentar iniciar um appointment `AGENDADO` depois de `startsAt + 10 minutos`
- **THEN** a API MUST responder `APPOINTMENT_NO_SHOW_DEADLINE` com HTTP 409 e MUST persistir `FALTA` de forma idempotente

#### Scenario: Cancelamento após o limite

- **WHEN** usuário tentar cancelar um appointment `AGENDADO` depois de `startsAt + 10 minutos`
- **THEN** a API MUST responder `APPOINTMENT_NO_SHOW_DEADLINE` com HTTP 409 e MUST persistir `FALTA` de forma idempotente

### Requirement: O monitor automático marca faltas de forma idempotente

O serviço MUST monitorar appointments `AGENDADO` cujo prazo de dez minutos foi atingido e MUST alterar somente esses registros para `FALTA`, sem modificar appointments já iniciados, cancelados ou finalizados.

#### Scenario: Execução periódica encontra faltas

- **WHEN** uma execução do monitor ocorrer e existirem appointments `AGENDADO` com `startsAt + 10 minutos <= agora`
- **THEN** todos os candidatos MUST poder ser marcados como `FALTA` sem ultrapassar o limite e sem depender de requisição do frontend

#### Scenario: Monitor executado novamente

- **WHEN** duas execuções do monitor processarem o mesmo intervalo ou houver mais de uma instância da API
- **THEN** somente a primeira atualização efetiva MUST alterar cada appointment e o resultado final MUST permanecer `FALTA` sem erro de duplicidade

#### Scenario: Atendimento iniciado não vira falta

- **WHEN** o monitor encontrar appointment em `ATENDENDO`, `CANCELADO` ou `FINALIZADO`
- **THEN** MUST ignorar o registro e MUST preservar seu status

### Requirement: A leitura reconcilia o prazo automático

As consultas de listagem e detalhe MUST retornar `FALTA` para appointment `AGENDADO` cujo prazo já tenha vencido, mesmo quando o monitor periódico ainda não tiver executado, e MUST manter o resultado compatível com a persistência.

#### Scenario: Detalhe consultado após o prazo

- **WHEN** `GET /api/appointments/{id}` for executado após `startsAt + 10 minutos` para appointment ainda `AGENDADO`
- **THEN** a API MUST reconciliar o estado para `FALTA` antes de responder

#### Scenario: Lista consultada após o prazo

- **WHEN** `GET /api/appointments` incluir appointment `AGENDADO` cujo prazo já venceu
- **THEN** o item em `content` MUST retornar `FALTA` e a alteração MUST ser persistida de forma idempotente

### Requirement: O monitor fornece observabilidade operacional sem dados sensíveis

Cada execução automática MUST registrar evento operacional com início/fim ou duração, quantidade de candidatos, quantidade atualizada e erro quando houver, sem paciente, token, payload completo ou outro dado pessoal.

#### Scenario: Execução sem erro

- **WHEN** o monitor concluir uma rodada sem falha
- **THEN** o log MUST permitir identificar a rodada, sua duração, candidatos encontrados e registros atualizados

#### Scenario: Falha no monitor

- **WHEN** a rodada falhar por indisponibilidade ou erro de persistência
- **THEN** o log MUST registrar a falha e a próxima execução MUST continuar possível sem corromper status já persistidos

## REMOVED Requirements

### Requirement: Estado atual não possui transições HTTP

**Reason**: O ciclo inicial de atendimento passa a exigir início, cancelamento e falta automática persistidos para integração com a agenda web.

**Migration**: Consumidores devem usar `PATCH /api/appointments/{id}/status` para `ATENDENDO` ou `CANCELADO`; `FALTA` é reservado ao monitor e à reconciliação automática.

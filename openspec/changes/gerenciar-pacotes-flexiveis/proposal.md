# Proposal

## Why

A API ainda não possui uma definição persistente e autorizada de pacote de serviço por unidade. Esse recurso é necessário antes de o frontend oferecer a gestão administrativa e antes da futura geração automática de agendamentos.

## What Changes

- Criar o recurso administrativo de pacotes associados a serviço e unidade da clínica autenticada.
- Persistir total de sessões, preço decimal, dias semanais únicos, situação e auditoria; a frequência é derivada dos dias, não um campo independente.
- Expor operações autenticadas de criação, consulta, atualização e mudança de situação, limitadas a `ADMIN`, sem `DELETE`.
- Impedir alteração de pacote que tenha vínculo de agendamento de pacote, a ser introduzido pela SPEC 02; até lá, não criar ocorrências nem profissional.

## Capabilities

### New Capabilities

- `service-package-management`: mantém e protege definições de pacotes por unidade, incluindo configuração semanal e situação administrativa.

### Modified Capabilities

- Nenhuma.

## Impact

- Nova migration Flyway, entidades, repositório, DTOs, serviço e controller.
- Novos endpoints sob `/api/service-packages`; não altera o contrato manual de `/api/appointments`.
- Depende do escopo de unidade implementado em `UnitScopeService` e deve ser consumido pelo frontend coordenadamente.

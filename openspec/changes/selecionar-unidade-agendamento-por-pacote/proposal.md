# Proposal

## Why

O frontend passou a exigir uma unidade efetiva no agendamento por pacote, mas a
API ainda aceita um request sem `unitId`, valida paciente apenas pela clínica e
protege a listagem de pacotes exclusivamente para ADMIN. Assim, usuários BASIC
não conseguem carregar os pacotes da própria unidade e uma requisição alterada
pode misturar unidade, paciente e pacote.

## What Changes

- Exigir `unitId` na prévia e na confirmação de agendamento por pacote.
- Validar que a unidade pertence à clínica e está no escopo do usuário; ADMIN
  pode usar unidades da própria clínica e BASIC somente sua unidade associada.
- Validar que pacote ativo, paciente e unidade são compatíveis antes da prévia,
  confirmação e leitura da contratação.
- Permitir a leitura filtrada de pacotes ativos para usuários autenticados,
  mantendo criação, edição, ativação e detalhe administrativo restritos a ADMIN.
- Incluir a unidade no fingerprint da prévia para impedir confirmação em outro
  contexto.

Fora de escopo: criar unidades, alterar a associação do usuário, modificar a
  tabela de contratos, adicionar calendário operacional ou mudar as regras do
  agendamento manual.

## Capabilities

### New Capabilities

- `package-appointment-unit-scoping`: valida e aplica o escopo de unidade aos
  catálogos e às operações de contratação por pacote.

### Modified Capabilities

Nenhuma.

## Impact

- DTO e serviço de contratação por pacote, incluindo prévia, confirmação e
  consulta por ID.
- Controller e serviço de pacotes, separando leitura operacional de mutações
  administrativas.
- Testes unitários e de integração de autorização, isolamento por unidade e
  compatibilidade entre paciente e pacote.
- Contrato HTTP: `unitId` passa a ser obrigatório no corpo de prévia e
  confirmação; não há alteração de schema ou migration.

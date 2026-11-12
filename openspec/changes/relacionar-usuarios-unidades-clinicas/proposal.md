# Proposal

## Why

Os usuários atualmente pertencem apenas à clínica, e as consultas de agenda permitem o escopo de qualquer unidade da clínica para todo usuário autenticado. Uma associação explícita com `clinic_unit` é necessária para que usuários não administradores operem somente na sua unidade, enquanto administradores preservam o acesso de toda a clínica.

## What Changes

- Adicionar associação de cada usuário a uma `clinic_unit` da própria clínica e validar essa consistência.
- Definir que `ADMIN` tem escopo de todas as unidades da sua clínica e que usuários não administradores têm escopo somente da unidade associada.
- Migrar cada usuário existente para a unidade `MATRIZ` da sua própria clínica, sem reduzir os privilégios de administradores.
- **BREAKING** Restringir o cadastro e a alteração de associação de usuários a `ADMIN`, exigindo clínica e unidade no contrato administrativo de criação.
- Estender o perfil de sessão com os identificadores e nomes da clínica e da unidade autorizada.
- **BREAKING** Alterar `GET /patient/list` para exigir autenticação e aplicar o escopo efetivo da identidade às consultas de pacientes; o frontend passará a usar esse endpoint em vez de `/api/patients`.
- Aplicar o escopo efetivo da identidade autenticada às consultas de agendamentos, sem confiar em parâmetros enviados pelo cliente.

## Capabilities

### New Capabilities

- `user-clinic-unit-assignment`: relacionamento entre usuário e unidade clínica, gestão administrativa e migração dos usuários existentes.

### Modified Capabilities

- `authentication-and-users`: o perfil e o cadastro de usuário passam a conter e administrar o contexto de unidade.
- `appointments`: consultas e criação de agendamentos passam a respeitar a unidade efetiva de usuários não administradores.
- `patients`: a leitura de pacientes passa a exigir autenticação e respeitar o escopo de unidade do usuário.

## Impact

- Banco de dados: coluna de unidade em `USERS`, chave estrangeira, validação de pertencimento e migration de preenchimento.
- API: contratos de perfil/sessão, cadastro de usuários e listagens de pacientes/agendamentos serão alterados; clientes precisam consumir os novos campos e não depender de listagens globais.
- Segurança: regras de escopo são aplicadas no backend a partir do principal autenticado.
- Fora de escopo: seletor de unidade para usuários comuns, administração completa de filiais e alteração das regras de status de clínica/unidade.

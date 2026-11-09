# clinic-structure Specification

## Purpose
Define a criação inicial da clínica e as invariantes de pertencimento, tipo e status de suas unidades, com uma MATRIZ obrigatória por clínica.

## Requirements

### Requirement: Cadastro inicial cria clínica e MATRIZ ativas

`POST /auth/register/clinic` MUST criar, como uma única unidade de trabalho, uma clínica `ATIVO`, exatamente uma unidade `MATRIZ` `ATIVO` pertencente a ela e a equipe e contas iniciais definidas na capacidade `professionals`. Nesta fase, o cadastro inicial MUST criar apenas a MATRIZ e responder HTTP 201 sem expor senhas ou hashes.

#### Scenario: Cadastro inicial completo

- **WHEN** são informados dados válidos da clínica, da MATRIZ e das duas pessoas com credenciais distintas
- **THEN** a clínica, a MATRIZ, os dois profissionais e suas duas contas são criados juntos, com os status iniciais `ATIVO`

#### Scenario: Falha em uma conta inicial

- **WHEN** o login de uma das contas iniciais já existe
- **THEN** o cadastro é rejeitado e nenhum dos registros da nova clínica permanece criado

### Requirement: Clínica possui uma única MATRIZ

Cada unidade MUST pertencer a exatamente uma clínica, e cada clínica MUST possuir uma ou mais unidades e exatamente uma unidade do tipo `MATRIZ`. `TypeUnit` MUST admitir `MATRIZ` e `FILIAL`; a criação de `FILIAL` fica fora do fluxo desta fase.

#### Scenario: Estrutura recém-criada

- **WHEN** o cadastro inicial é concluído
- **THEN** a clínica possui uma única unidade, do tipo `MATRIZ`, e essa unidade pertence somente à clínica criada

#### Scenario: Segunda MATRIZ

- **WHEN** uma gravação tenta associar uma segunda unidade `MATRIZ` à mesma clínica
- **THEN** a operação é rejeitada e a clínica conserva uma única MATRIZ

### Requirement: Estados de clínica e unidade são explícitos

`StatusClinic` e `StatusUnit` MUST admitir `ATIVO`, `INATIVO` e `BLOQUEADA`. A criação inicial MUST definir ambos como `ATIVO`; esta fase não oferece operação para mudar esses estados.

#### Scenario: Estados iniciais

- **WHEN** uma clínica é criada pelo cadastro inicial
- **THEN** a clínica e sua MATRIZ são persistidas com status `ATIVO`

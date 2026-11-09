# professionals Specification

## Purpose
Define o pertencimento de profissionais às unidades da clínica e a equipe operacional criada no cadastro inicial da MATRIZ.

## Requirements

### Requirement: Profissional pertence a uma única unidade da clínica

Cada profissional MUST pertencer a exatamente uma clínica e a exatamente uma unidade dessa mesma clínica. Uma unidade MUST possuir ao menos um profissional. Nesta fase, o mesmo profissional não pode estar associado a duas unidades.

#### Scenario: Vínculo válido

- **WHEN** um profissional é registrado para uma unidade de sua clínica
- **THEN** seu cadastro identifica uma única unidade e uma única clínica coerentes entre si

#### Scenario: Vínculo entre clínicas

- **WHEN** uma operação tenta vincular profissional a unidade de outra clínica
- **THEN** a operação é rejeitada e o vínculo anterior é preservado

#### Scenario: Segunda unidade para o mesmo profissional

- **WHEN** uma operação tenta associar o mesmo profissional a outra unidade
- **THEN** a operação é rejeitada nesta fase

### Requirement: MATRIZ nasce com ATENDENTE e ADMINISTRATIVO

O cadastro inicial MUST criar na MATRIZ um profissional do tipo `ATENDENTE` e outro do tipo `ADMINISTRATIVO`, valores de `TypeProfessional`. Cada um MUST ter uma conta de usuário própria, vinculada à mesma clínica, com login e senha informados no cadastro inicial. A conta do `ADMINISTRATIVO` MUST ter papel `ADMIN`; a do `ATENDENTE` MUST ter papel `USER`.

#### Scenario: Equipe inicial completa

- **WHEN** o cadastro inicial da clínica é concluído
- **THEN** a MATRIZ possui os dois profissionais de tipos distintos e cada profissional tem sua própria conta apta a login

#### Scenario: Falha em profissional inicial

- **WHEN** não é possível criar ou vincular um dos profissionais iniciais
- **THEN** o cadastro inicial inteiro é rejeitado, sem deixar clínica ou contas parciais

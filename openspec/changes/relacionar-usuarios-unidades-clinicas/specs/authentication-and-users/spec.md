# Spec Delta

## MODIFIED Requirements

### Requirement: Perfil e listagem de usuários
`GET /auth/me` MUST devolver o identificador e os nomes da clínica e da unidade ativa do usuário autenticado. Para `ADMIN`, a unidade ativa é apenas o contexto padrão e não restringe o escopo de unidades da clínica. `GET /auth/list` MUST listar somente usuários da clínica do principal e MUST exigir `ADMIN`.

#### Scenario: Consulta de perfil
- **WHEN** um usuário autenticado chama `GET /auth/me`
- **THEN** recebe identificadores e nomes de sua clínica, tenant e unidade ativa

#### Scenario: Listagem por administrador
- **WHEN** um `ADMIN` chama `GET /auth/list`
- **THEN** recebe somente usuários da sua clínica

#### Scenario: Listagem de usuários
- **WHEN** um administrador chama `GET /auth/list`
- **THEN** a consulta retorna somente usuários da clínica do principal

#### Scenario: Listagem por usuário não administrador
- **WHEN** um usuário sem papel `ADMIN` chama `GET /auth/list`
- **THEN** a API nega a operação sem expor usuários

### Requirement: Cadastro legado de usuário
`POST /user/register/{clinicId}` MUST exigir `ADMIN` autenticado da clínica indicada, login e senha novos, `isProfessional` e `unitId` obrigatório pertencente àquela clínica. O usuário criado MUST ter papel `USER` e associação à unidade informada; se profissional, o profissional MUST ser vinculado à mesma unidade. A senha MUST ser codificada e a resposta permanece HTTP 200 sem corpo.

#### Scenario: Cadastro por administrador
- **WHEN** um administrador da clínica envia login, senha, `isProfessional` e `unitId` compatível
- **THEN** a conta `USER` é criada com senha codificada e associação à unidade informada

#### Scenario: Cadastro por usuário autenticado
- **WHEN** administrador autenticado da clínica envia login novo, senha, `isProfessional=false` e unidade válida
- **THEN** a conta `USER` é criada com senha codificada e vínculo à unidade informada

#### Scenario: Cadastro profissional
- **WHEN** um administrador envia `isProfessional=true` e dados profissionais válidos
- **THEN** a conta e o profissional são criados juntos e vinculados à unidade informada

#### Scenario: Cadastro de usuário profissional
- **WHEN** administrador envia `isProfessional=true`, unidade válida e dados profissionais válidos
- **THEN** a conta `USER` e o profissional vinculado à unidade são criados juntos

#### Scenario: Unidade ausente ou de outra clínica
- **WHEN** a solicitação omite `unitId` ou informa unidade de outra clínica
- **THEN** a API rejeita o cadastro sem criar usuário ou profissional

#### Scenario: Escolha profissional ausente
- **WHEN** a requisição omite `isProfessional`
- **THEN** o cadastro é rejeitado sem criar usuário

#### Scenario: Clínica de outra pessoa
- **WHEN** administrador tenta cadastrar conta usando `clinicId` de outra clínica
- **THEN** a API rejeita a operação sem criar usuário ou profissional na outra clínica

#### Scenario: Segundo ADMIN
- **WHEN** o cadastro posterior solicita papel `ADMIN`
- **THEN** a API rejeita a operação e mantém o único `ADMIN` da clínica

#### Scenario: Cadastro por não administrador
- **WHEN** usuário autenticado sem papel `ADMIN` tenta cadastrar uma conta
- **THEN** a API rejeita a operação sem criar usuário

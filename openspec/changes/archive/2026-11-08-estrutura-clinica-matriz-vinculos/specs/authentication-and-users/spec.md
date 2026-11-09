# Spec Delta

## MODIFIED Requirements

### Requirement: Cadastro legado de usuário

`POST /user/register/{clinicId}` MUST exigir usuário autenticado da clínica indicada no caminho, login e senha novos e resposta obrigatória `isProfessional` (`true` ou `false`). O cadastro posterior MUST criar somente conta `USER`; se `isProfessional=true`, MUST criar e vincular um profissional à MATRIZ da mesma clínica com os dados profissionais necessários. Se `false`, MUST criar apenas a conta. A senha MUST ser codificada; a resposta permanece HTTP 200 sem corpo.

#### Scenario: Cadastro por usuário autenticado

- **WHEN** usuário autenticado da clínica envia login novo, senha e `isProfessional=false` para sua clínica
- **THEN** a conta `USER` é criada com senha codificada e sem vínculo profissional

#### Scenario: Cadastro de usuário profissional

- **WHEN** usuário autenticado da clínica envia login novo, senha, `isProfessional=true` e dados profissionais válidos
- **THEN** a conta `USER` e o profissional vinculado à MATRIZ são criados juntos na mesma clínica

#### Scenario: Escolha profissional ausente

- **WHEN** a requisição omite `isProfessional`
- **THEN** o cadastro é rejeitado sem criar usuário

#### Scenario: Clínica de outra pessoa

- **WHEN** usuário autenticado tenta cadastrar conta usando `clinicId` de outra clínica
- **THEN** a API rejeita a operação sem criar usuário ou profissional na outra clínica

#### Scenario: Segundo ADMIN

- **WHEN** o cadastro posterior solicita papel `ADMIN` em clínica que já possui seu administrador inicial
- **THEN** a API rejeita a operação e mantém um único `ADMIN` na clínica

## ADDED Requirements

### Requirement: Cada usuário pertence a uma clínica

Cada usuário MUST estar associado a exatamente uma clínica. A criação de uma conta MUST impedir vínculo simultâneo com clínicas diferentes.

#### Scenario: Associação única

- **WHEN** uma conta é criada para uma clínica
- **THEN** seu cadastro e autenticação identificam somente essa clínica

### Requirement: Um único ADMIN por clínica

Cada clínica MUST possuir exatamente uma conta com papel `ADMIN`, criada no cadastro inicial e vinculada ao profissional `ADMINISTRATIVO` da MATRIZ. O cadastro posterior MUST impedir outro `ADMIN`; a substituição dessa conta não faz parte desta fase.

#### Scenario: Administrador inicial

- **WHEN** o cadastro inicial da clínica termina
- **THEN** há exatamente um usuário `ADMIN` naquela clínica

#### Scenario: Administradores de clínicas diferentes

- **WHEN** duas clínicas são cadastradas
- **THEN** cada uma possui seu próprio e único `ADMIN`

### Requirement: Status da clínica ou unidade não impede login

A autenticação MUST permitir login válido de usuário de clínica ou unidade `ATIVO`, `INATIVO` ou `BLOQUEADA`. As restrições de appointment por status são aplicadas na operação de agenda, não no login.

#### Scenario: Conta vinculada a unidade inativa

- **WHEN** usuário com credenciais válidas pertence a uma clínica ou unidade `INATIVO`
- **THEN** o login é concluído e a conta pode acessar as operações ainda permitidas

#### Scenario: Conta vinculada a unidade bloqueada

- **WHEN** usuário com credenciais válidas pertence a uma clínica ou unidade `BLOQUEADA`
- **THEN** o login é concluído e a conta pode acessar as operações ainda permitidas


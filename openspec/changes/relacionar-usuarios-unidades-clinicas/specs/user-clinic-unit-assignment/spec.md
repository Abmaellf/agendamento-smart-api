# Spec Delta

## Purpose

Define a associação verificável entre usuário e unidade clínica, o escopo operacional derivado dessa associação e a migração segura dos usuários já existentes.

## ADDED Requirements

### Requirement: Usuário não administrador possui uma unidade da própria clínica
O sistema MUST manter para todo usuário não administrador uma `clinic_unit` pertencente à sua clínica e rejeitar associação a unidade de outra clínica.

#### Scenario: Associação compatível
- **WHEN** um usuário não administrador é associado a uma unidade da sua clínica
- **THEN** a associação MUST ser persistida e compor o contexto autenticado

#### Scenario: Associação entre clínicas
- **WHEN** uma operação associa usuário a unidade de clínica diferente
- **THEN** o sistema MUST rejeitar a operação sem alterar o vínculo vigente

### Requirement: Administrador tem escopo de todas as unidades da clínica
O sistema MUST resolver o escopo de um usuário `ADMIN` como todas as unidades pertencentes à sua clínica, independentemente da unidade padrão persistida.

#### Scenario: Administrador solicita contexto de unidades
- **WHEN** um administrador autenticado opera sobre unidades da própria clínica
- **THEN** o sistema MUST autorizar qualquer uma dessas unidades conforme as demais regras aplicáveis

### Requirement: Somente administrador gerencia vínculo de unidade
O sistema MUST permitir criar ou alterar associação de usuário a clínica e unidade somente a `ADMIN` autenticado e MUST exigir clínica e unidade no cadastro administrativo.

#### Scenario: Cadastro administrativo completo
- **WHEN** um administrador informa clínica e unidade compatíveis ao criar usuário
- **THEN** o usuário MUST ser criado com o vínculo informado

#### Scenario: Cadastro incompleto ou por não administrador
- **WHEN** a solicitação omite clínica/unidade ou é realizada por usuário sem papel `ADMIN`
- **THEN** o sistema MUST rejeitá-la sem criar ou alterar vínculos

### Requirement: Usuários existentes recebem unidade inicial
O sistema MUST associar cada usuário existente à unidade `MATRIZ` da sua própria clínica, preservando os demais atributos e o escopo administrativo.

#### Scenario: Execução da migração
- **WHEN** a migração é aplicada a usuários existentes
- **THEN** cada usuário MUST possuir a `MATRIZ` da sua clínica e administradores MUST continuar com escopo de todas as unidades da clínica

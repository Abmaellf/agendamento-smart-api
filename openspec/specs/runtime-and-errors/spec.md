# Runtime and Errors Specification

## Purpose

Comportamento operacional e erros HTTP observados em `pom.xml`, configurações Spring, migrations, `ApiExceptionHandler`, `BuildInfoController` e `DebugController`.

## Requirements

### Requirement: Schema PostgreSQL é versionado por Flyway

A aplicação MUST usar PostgreSQL e Flyway; Hibernate valida o schema com `ddl-auto=validate`. `test` carrega migrations e seeds de desenvolvimento por padrão; `homo` e `prod` carregam somente migrations. `V009` renomeia `SCHEDULING` para `APPOINTMENT`.

#### Scenario: Inicialização no profile test

- **WHEN** API inicia com conexão PostgreSQL e profile `test`
- **THEN** Flyway usa `classpath:db/migration,classpath:db/seed/dev`, salvo override por `AGENDA_FLYWAY_LOCATIONS`

### Requirement: Configuração obrigatória é externa

Runtime MUST receber `AGENDA_URL`, `AGENDA_DB_USER`, `AGENDA_DB_PASSWORD` e `JWT_SECRET` do ambiente. A API escuta na porta 8080 na configuração atual.

#### Scenario: Ambiente local configurado

- **WHEN** variáveis são fornecidas e banco está acessível
- **THEN** aplicação pode inicializar e validar o schema existente

### Requirement: Erros de domínio e validação têm resposta estruturada

`DomainException` MUST devolver `code`, `message` e `fieldErrors` com o status definido no erro. Falha de validação do corpo devolve `VALIDATION_ERROR` com HTTP 400; corpo ilegível devolve `INVALID_REQUEST` com HTTP 400. Exceções não cobertas pelo advice não têm contrato uniforme nesta linha de base.

#### Scenario: Campo inválido na criação de agenda

- **WHEN** constraint Jakarta Validation falha
- **THEN** resposta HTTP 400 inclui `VALIDATION_ERROR` e erros por campo

### Requirement: Endpoints de diagnóstico exigem autenticação

`GET /api/build-info/version` MUST devolver versão do build quando disponível ou versão do projeto. `GET /debug/db` MUST devolver nome do banco da conexão. Ambas seguem autenticação padrão.

#### Scenario: Usuário autenticado consulta versão

- **WHEN** chama `GET /api/build-info/version`
- **THEN** recebe string de versão

# Recursos e configuração da aplicação

## Objetivo do módulo

Centralizar configuração de runtime e migrations empacotadas no classpath.

## Responsabilidades principais

- Configurar datasource PostgreSQL, JPA/Hibernate, Flyway, porta, logs e segredo JWT.
- Disponibilizar migrations em `db/migration` e seeds de desenvolvimento separadas em `db/seed/dev`.
- Receber a versão Maven por resource filtering.

## Funcionalidades existentes

- Datasource por variáveis de ambiente.
- Driver PostgreSQL explícito; o dialeto é detectado pelo Hibernate a partir da conexão JDBC.
- Hibernate apenas valida o schema (`ddl-auto=validate`) e usa UTC no JDBC.
- Flyway `11.14.1` habilitado com `baseline-on-migrate=false` e `out-of-order=false`.
- Somente `classpath:db/migration` é carregado por padrão; o Compose adiciona `classpath:db/seed/dev` via `AGENDA_FLYWAY_LOCATIONS`.
- API na porta 8080.
- SQL e JDBC em níveis detalhados de log.

## Dependências internas e externas

- Internas: `TokenService`, `ProjectInfoProperties`, entidades JPA e migrations.
- Externas: Spring Boot, PostgreSQL, Hibernate e Flyway.

## Módulos relacionados

Todo o runtime da aplicação, Docker Compose e `pom.xml`.

## Pontos de entrada

- Spring Boot lê `application.yaml` na inicialização.
- Flyway descobre `classpath:db/migration` por padrão; locais adicionais dependem de `AGENDA_FLYWAY_LOCATIONS`.

## Fluxos de entrada

Ambiente/Maven -> properties -> autoconfiguração Spring -> datasource/JPA/Flyway/security/logs.

## Arquivos críticos

- `application.yaml`.
- `db/migration/*.sql`.
- `db/seed/dev/*.sql` (somente dados de desenvolvimento habilitados explicitamente).

## Regras confirmadas para evolução do módulo

- Configuração é separada por ambiente e não contém credenciais ou segredo funcional no repositório.
- Datas de agendamento são persistidas com semântica de fuso explícito; o formato definitivo será fechado na entrevista.
- Logs de produção não exibem SQL com dados pessoais, tokens, CPF/CNPJ, motivos ou payloads sensíveis.
- Flyway continua como fonte de verdade do schema; inicialização deve falhar de forma clara diante de migration incompatível.
- Configurações de segurança de cookie/token devem ser próprias de cada ambiente.

## Observações técnicas e débitos identificados

- `JWT_SECRET` possui fallback `my-secret-key`.
- Não há profiles separados para local, teste e produção.
- `show-sql`, Hibernate SQL `DEBUG` e JDBC `DEBUG` estão habilitados globalmente.
- O caminho padrão não executa seeds; o `docker-compose.yml` opta por elas ao definir `AGENDA_FLYWAY_LOCATIONS`.
- O volume PostgreSQL `postgresql_data` não reutiliza nem converte o volume MySQL anterior.
- O resource filtering aplica-se a todos os recursos, aumentando a necessidade de cuidado com placeholders.

# Recursos e configuração da aplicação

## Objetivo do módulo

Centralizar configuração de runtime e migrations empacotadas no classpath.

## Responsabilidades principais

- Configurar datasource MySQL, JPA/Hibernate, Flyway, porta, logs e segredo JWT.
- Disponibilizar migrations em `db/migration`.
- Receber a versão Maven por resource filtering.

## Funcionalidades existentes

- Datasource por variáveis de ambiente.
- Hibernate sem geração de schema.
- Flyway habilitado com baseline e migrations fora de ordem.
- API na porta 8082.
- SQL e JDBC em níveis detalhados de log.

## Dependências internas e externas

- Internas: `TokenService`, `ProjectInfoProperties`, entidades JPA e migrations.
- Externas: Spring Boot, MySQL, Hibernate e Flyway.

## Módulos relacionados

Todo o runtime da aplicação, Docker Compose e `pom.xml`.

## Pontos de entrada

- Spring Boot lê `application.properties` na inicialização.
- Flyway descobre `classpath:db/migration`.

## Fluxos de entrada

Ambiente/Maven -> properties -> autoconfiguração Spring -> datasource/JPA/Flyway/security/logs.

## Arquivos críticos

- `application.properties`.
- `db/migration/*.sql`.

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
- `spring.flyway.out-of-order=true` permite aplicar migrations antigas adicionadas depois.
- O resource filtering aplica-se a todos os recursos, aumentando a necessidade de cuidado com placeholders.

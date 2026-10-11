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
- Profiles Spring separados para teste (`test`), homologação (`homo`) e produção (`prod`).
- Profiles Maven homônimos como atalhos para selecionar o profile Spring em `spring-boot:run`.
- O profile `test` carrega migrations e seeds de desenvolvimento por padrão; `homo` e `prod` carregam somente migrations.
- API na porta 8080.
- SQL em `DEBUG` somente em `test`, sem registrar valores dos parâmetros; os demais profiles usam níveis seguros.
- Segredo JWT obrigatório por `JWT_SECRET`, sem fallback no repositório.

## Dependências internas e externas

- Internas: `TokenService`, `ProjectInfoProperties`, entidades JPA e migrations.
- Externas: Spring Boot, PostgreSQL, Hibernate e Flyway.

## Módulos relacionados

Todo o runtime da aplicação, Docker Compose e `pom.xml`.

## Pontos de entrada

- Spring Boot lê `application.yaml` e o `application-<profile>.yaml` selecionado pelo runtime, pelo IntelliJ ou pelo atalho Maven.
- Flyway descobre os locais definidos pelo profile; `AGENDA_FLYWAY_LOCATIONS` permite uma sobrescrita explícita.

## Fluxos de entrada

Ambiente/Maven -> properties -> autoconfiguração Spring -> datasource/JPA/Flyway/security/logs.

## Arquivos críticos

- `application.yaml` e `application-{test,homo,prod}.yaml`.
- `db/migration/*.sql`.
- `db/seed/dev/*.sql` (dados de desenvolvimento habilitados pelo profile `test` ou por sobrescrita explícita).

## Regras confirmadas para evolução do módulo

- Configuração é separada por ambiente e não contém credenciais ou segredo funcional no repositório.
- Datas de agendamento são persistidas com semântica de fuso explícito; o formato definitivo será fechado na entrevista.
- Logs de produção não exibem SQL com dados pessoais, tokens, CPF/CNPJ, motivos ou payloads sensíveis.
- Flyway continua como fonte de verdade do schema; inicialização deve falhar de forma clara diante de migration incompatível.
- Configurações de segurança de cookie/token devem ser próprias de cada ambiente.

## Observações técnicas e débitos identificados

- Nenhum profile é ativado implicitamente fora do Compose; selecione-o pelo runtime, pelo IntelliJ ou por `./mvnw -P<profile> spring-boot:run`.
- O `docker-compose.yml` ativa `test`, que inclui as seeds de desenvolvimento; ele não é uma configuração de homologação ou produção.
- O volume PostgreSQL `postgresql_data` não reutiliza nem converte o volume MySQL anterior.
- O resource filtering aplica-se a todos os recursos, aumentando a necessidade de cuidado com placeholders.

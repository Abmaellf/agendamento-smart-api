# Recursos de banco de dados

## Objetivo do módulo

Agrupar os recursos de evolução do banco carregados pelo classpath.

## Responsabilidades principais

- Delimitar o diretório Flyway `db/migration` configurado pela aplicação.

## Funcionalidades existentes

- Nove migrations versionadas para o schema PostgreSQL e seeds repetíveis de desenvolvimento em local separado.

## Dependências internas e externas

- Internas: `application.yaml` aponta por padrão para `classpath:db/migration`.
- Externas: Flyway `11.14.1` e PostgreSQL.

## Módulos relacionados

`db/migration`, modelos JPA e repositories.

## Pontos de entrada

Descoberta automática do Flyway durante a inicialização.

## Fluxos de entrada

Classpath -> Flyway -> migrations pendentes -> schema PostgreSQL -> validação do schema pelo Hibernate.

## Arquivos críticos

- `migration/V001__create_table_clinic.sql` até a `V009`, responsável pelo rename compatível para `APPOINTMENT`.
- `seed/dev/R__001_seed_clinic.sql` até a seed histórica `seed/dev/R__008_seed_scheduling.sql`.

## Regras confirmadas para evolução do módulo

- O schema-alvo inclui tenant/clínica, unidade, usuário, paciente, profissional, serviço, agendamento, série e evento.
- Todo dado de negócio possui tenant e timestamps; agendamento possui unidade e snapshots de duração/preço.
- Constraints e índices devem reforçar unicidades e consultas de conflito/capacidade.
- Histórico de agendamento não pode ser removido por cascade.
- Mudanças são sempre novas migrations e devem ser testadas em PostgreSQL vazio.
- Migração de um ambiente MySQL existente é um processo de dados separado; o Flyway e o volume `postgresql_data` não importam o volume anterior automaticamente.

## Observações técnicas e débitos identificados

- As seeds de desenvolvimento ficam em `db/seed/dev` e não fazem parte do caminho padrão da aplicação. O Compose as habilita por `AGENDA_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/seed/dev`.
- Os achados por migration estão em `migration/README.md`.

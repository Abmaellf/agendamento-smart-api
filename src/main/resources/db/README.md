# Recursos de banco de dados

## Objetivo do módulo

Agrupar os recursos de evolução do banco carregados pelo classpath.

## Responsabilidades principais

- Delimitar o diretório Flyway `db/migration` configurado pela aplicação.

## Funcionalidades existentes

- Cinco migrations versionadas para o schema MySQL.

## Dependências internas e externas

- Internas: `application.properties` aponta para `classpath:db/migration`.
- Externas: Flyway e MySQL.

## Módulos relacionados

`db/migration`, modelos JPA e repositories.

## Pontos de entrada

Descoberta automática do Flyway durante a inicialização.

## Fluxos de entrada

Classpath -> Flyway -> migrations pendentes -> schema MySQL.

## Arquivos críticos

- `migration/V001__create_table_clinic.sql` até `migration/V005__alter_scheduling.sql`.

## Regras confirmadas para evolução do módulo

- O schema-alvo inclui tenant/clínica, unidade, usuário, paciente, profissional, serviço, agendamento, série e evento.
- Todo dado de negócio possui tenant e timestamps; agendamento possui unidade e snapshots de duração/preço.
- Constraints e índices devem reforçar unicidades e consultas de conflito/capacidade.
- Histórico de agendamento não pode ser removido por cascade.
- Mudanças são sempre novas migrations e devem ser testadas em MySQL vazio e em atualização do schema legado.

## Observações técnicas e débitos identificados

- Não há scripts separados de seed por ambiente; a clínica inicial faz parte da migration de schema.
- Os achados por migration estão em `migration/README.md`.

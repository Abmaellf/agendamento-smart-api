# Migrations do banco

## Objetivo do módulo

Versionar o schema MySQL utilizado pela API.

## Responsabilidades principais

- Criar tabelas, chaves, índices, relacionamentos e defaults.
- Inserir a clínica inicial.
- Manter compatibilidade entre o modelo JPA e o banco.

## Funcionalidades existentes

- `V001`: cria `CLINIC` e insere `Clínica Central` com UUID fixo.
- `V002`: cria `USERS`, FK de clínica e índice único de login.
- `V003`: cria `PATIENT`, código único e FK restritiva de clínica.
- `V004`: cria `SCHEDULING`, JSON de patologias, status e FK com cascade para paciente.
- `V005`: recria a chave primária de `SCHEDULING` mantendo `BINARY(16)`.

## Dependências internas e externas

- Internas: entidades e repositories devem permanecer compatíveis com esse schema.
- Externas: Flyway 10.20.0 no runtime e MySQL 8.0.

## Módulos relacionados

`model/clinic`, `model/user`, `model/patient`, `model/scheduling`, `repository` e `application.properties`.

## Pontos de entrada

- Flyway executa os arquivos por versão durante a inicialização da aplicação.

## Fluxos de entrada

Startup -> conexão MySQL -> histórico Flyway -> migrations pendentes em ordem/configuração permitida -> schema disponível ao JPA.

## Arquivos críticos

- `V001__create_table_clinic.sql`.
- `V002__create_table_users.sql`.
- `V003__create_table_patient.sql`.
- `V004__create_create_scheduling.sql`.
- `V005__alter_scheduling.sql`.

## Regras confirmadas para evolução do módulo

- Novas migrations devem introduzir `Unit`, `Professional`, `Service`, `AppointmentSeries` e `AppointmentEvent`, além dos campos de tenant/auditoria ausentes.
- O enum persistido deve migrar para `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO` sem perder registros existentes.
- Agendamento passa a guardar unidade, serviço, profissional opcional, início com fuso explícito, duração e preço aplicados e vínculos de remarcação/série.
- O cascade que apaga `SCHEDULING` ao excluir paciente é incompatível com a regra de histórico permanente e deve ser removido por nova migration.
- Unicidades confirmadas: e-mail e CPF/CNPJ do contratante na plataforma; CPF do paciente e CPF/CREFITO do profissional por tenant.
- Migrations aplicadas permanecem imutáveis; toda correção ocorre em uma nova versão com teste de upgrade.

## Observações técnicas e débitos identificados

- O `ENUM` de `SCHEDULING.status` não contém `ATENDENDO`, embora o enum Java contenha.
- `V001` insere uma linha sem cláusula idempotente própria; `CREATE TABLE IF NOT EXISTS` não torna o `INSERT` idempotente fora do controle normal do Flyway.
- O UUID da seed usa bytes por `UNHEX(REPLACE(...))`; a query nativa `findByUuid` usa `UUID_TO_BIN(..., 1)`, com ordenação de bytes diferente.
- Paciente não possui índice/unique de nome ou composto por clínica; a regra de duplicidade existe apenas na aplicação.
- `V005` remove e adiciona novamente uma chave primária já definida em `V004`, sem outra alteração de tipo efetiva.
- `ON DELETE CASCADE` em agendamento e `ON DELETE RESTRICT` em paciente/clínica representam políticas diferentes que devem ser preservadas conscientemente.
- Novos campos/enum persistentes devem ser adicionados somente por nova migration; migrations aplicadas não devem ser editadas.

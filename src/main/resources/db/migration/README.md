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
- `V004`: cria `CLINIC_UNIT`.
- `V005`: cria `SERVICE_OFFERING`.
- `V006`: cria `PROFESSIONAL`.
- `V007`: cria a associação entre profissionais e serviços.
- `V008__create_table_scheduling.sql`: migration histórica que criou a estrutura legada de agendamentos; permanece imutável para preservar o checksum do Flyway.
- `V009`: renomeia a tabela para `APPOINTMENT`, a coluna temporal legada para `appointment_date` e os nomes físicos associados.

## Dependências internas e externas

- Internas: entidades e repositories devem permanecer compatíveis com esse schema.
- Externas: Flyway 10.20.0 no runtime e MySQL 8.0.

## Módulos relacionados

`model/appointment`, `model/clinic`, `model/user`, `model/patient`, `repository` e `application.yaml`.

## Pontos de entrada

- Flyway executa os arquivos por versão durante a inicialização da aplicação.

## Fluxos de entrada

Startup -> conexão MySQL -> histórico Flyway -> migrations pendentes em ordem/configuração permitida -> schema disponível ao JPA.

## Arquivos críticos

- `V001__create_table_clinic.sql`.
- `V002__create_table_users.sql`.
- `V003__create_table_patient.sql`.
- `V004__create_table_clinic_unit.sql`.
- `V005__create_table_service_offering.sql`.
- `V006__create_table_professional.sql`.
- `V007__create_table_professional_service.sql`.
- `V008__create_table_scheduling.sql` (histórico legado; não editar).
- `V009` de rename compatível para `APPOINTMENT`.

## Regras confirmadas para evolução do módulo

- Novas migrations devem introduzir `AppointmentSeries` e `AppointmentEvent` e manter os campos de tenant/auditoria consistentes.
- O enum persistido deve migrar para `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO` sem perder registros existentes.
- Agendamento passa a guardar unidade, serviço, profissional opcional, início com fuso explícito, duração e preço aplicados e vínculos de remarcação/série.
- As FKs de `APPOINTMENT` devem continuar restritivas para preservar o histórico.
- A `V009` produz `uk_appointment_clinic_idempotency`, FKs `fk_appointment_*`, checks `chk_appointment_*` e índices `idx_appointment_*`; no MySQL, a chave primária física permanece nomeada `PRIMARY`.
- Unicidades confirmadas: e-mail e CPF/CNPJ do contratante na plataforma; CPF do paciente e CPF/CREFITO do profissional por tenant.
- Migrations aplicadas permanecem imutáveis; toda correção ocorre em uma nova versão com teste de upgrade.

## Observações técnicas e débitos identificados

- A evolução do `ENUM` de `APPOINTMENT.status` deve permanecer sincronizada com `AppointmentStatus`.
- `V001` insere uma linha sem cláusula idempotente própria; `CREATE TABLE IF NOT EXISTS` não torna o `INSERT` idempotente fora do controle normal do Flyway.
- O UUID da seed usa bytes por `UNHEX(REPLACE(...))`; a query nativa `findByUuid` usa `UUID_TO_BIN(..., 1)`, com ordenação de bytes diferente.
- Paciente não possui índice/unique de nome ou composto por clínica; a regra de duplicidade existe apenas na aplicação.
- A seed repetível `R__008_seed_scheduling.sql` conserva o nome histórico por compatibilidade; seu conteúdo opera sobre `APPOINTMENT` depois da V009.
- Renomear migrations ou seeds já registradas pode alterar sua identidade/checksum no Flyway; qualquer mudança exige teste de upgrade, além do teste em banco vazio.
- Novos campos/enum persistentes devem ser adicionados somente por nova migration; migrations aplicadas não devem ser editadas.

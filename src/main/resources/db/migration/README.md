# Migrations do banco

## Objetivo do módulo

Versionar o schema PostgreSQL utilizado pela API.

## Responsabilidades principais

- Criar tabelas, chaves, índices, relacionamentos e defaults.
- Manter compatibilidade entre o modelo JPA e o banco.
- Usar tipos e restrições nativos do PostgreSQL.

## Funcionalidades existentes

- `V001`: cria `CLINIC`.
- `V002`: cria `USERS`, FK de clínica, check de papel e índice funcional único sobre `LOWER(login)`.
- `V003`: cria `PATIENT`, código único e FK restritiva de clínica.
- `V004`: cria `CLINIC_UNIT`.
- `V005`: cria `SERVICE_OFFERING`.
- `V006`: cria `PROFESSIONAL`.
- `V007`: cria a associação entre profissionais e serviços.
- `V008__create_table_scheduling.sql`: cria a estrutura de agendamentos ainda com o nome físico legado `SCHEDULING`.
- `V009`: renomeia a tabela para `APPOINTMENT`, a coluna temporal legada para `appointment_date` e os nomes físicos associados.

Os identificadores usam `UUID` nativo. `pathology` usa `jsonb`; `starts_at`, `created_at` e `updated_at` de agendamento usam `timestamptz` (`TIMESTAMP WITH TIME ZONE`). Campos que representam data/hora local sem deslocamento permanecem `TIMESTAMP WITHOUT TIME ZONE`/`TIME WITHOUT TIME ZONE`.

## Dependências internas e externas

- Internas: entidades e repositories devem permanecer compatíveis com esse schema.
- Externas: Flyway `11.14.1`, módulo `flyway-database-postgresql` e PostgreSQL 18.1 no ambiente validado.

## Módulos relacionados

`model/appointment`, `model/clinic`, `model/user`, `model/patient`, `repository` e `application.yaml`.

## Pontos de entrada

- Flyway executa os arquivos por versão durante a inicialização da aplicação.

## Fluxos de entrada

Startup -> conexão PostgreSQL -> histórico Flyway -> migrations pendentes em ordem -> schema validado pelo Hibernate/JPA.

`application.yaml` carrega apenas `classpath:db/migration` por padrão. O Compose define `AGENDA_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/seed/dev` para incluir as seeds repetíveis somente no ambiente local integrado.

## Arquivos críticos

- `V001__create_table_clinic.sql`.
- `V002__create_table_users.sql`.
- `V003__create_table_patient.sql`.
- `V004__create_table_clinic_unit.sql`.
- `V005__create_table_service_offering.sql`.
- `V006__create_table_professional.sql`.
- `V007__create_table_professional_service.sql`.
- `V008__create_table_scheduling.sql`.
- `V009` de rename compatível para `APPOINTMENT`.
- `../seed/dev/R__001_seed_clinic.sql` até `../seed/dev/R__008_seed_scheduling.sql` (dados opcionais de desenvolvimento).

## Regras confirmadas para evolução do módulo

- Novas migrations devem introduzir `AppointmentSeries` e `AppointmentEvent` e manter os campos de tenant/auditoria consistentes.
- O enum persistido deve migrar para `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO` sem perder registros existentes.
- Agendamento passa a guardar unidade, serviço, profissional opcional, início com fuso explícito, duração e preço aplicados e vínculos de remarcação/série.
- As FKs de `APPOINTMENT` devem continuar restritivas para preservar o histórico.
- A `V009` produz `pk_appointment`, `uk_appointment_clinic_idempotency`, FKs `fk_appointment_*`, checks `chk_appointment_*` e índices `idx_appointment_*`.
- Unicidades confirmadas: e-mail e CPF/CNPJ do contratante na plataforma; CPF do paciente e CPF/CREFITO do profissional por tenant.
- Migrations aplicadas permanecem imutáveis; toda correção ocorre em uma nova versão com teste de upgrade.

## Observações técnicas e débitos identificados

- A evolução do check de `APPOINTMENT.status` deve permanecer sincronizada com `AppointmentStatus`.
- Paciente não possui índice/unique de nome ou composto por clínica; a regra de duplicidade existe apenas na aplicação.
- A seed repetível `R__008_seed_scheduling.sql` conserva o nome histórico por compatibilidade; seu conteúdo opera sobre `APPOINTMENT` depois da V009.
- A clínica e os demais dados de demonstração pertencem às seeds de desenvolvimento, não às migrations de schema.
- O conjunto atual de migrations constrói um schema PostgreSQL novo; não converte o banco, o histórico Flyway, os dados nem o volume MySQL anterior. Preservação de dados exige exportação, transformação e importação explícitas.
- Não use `flyway repair` cegamente para aceitar checksums do histórico anterior: o comando só ajusta metadados e não converte schema ou dados. Confirme o banco alvo e o histórico esperado antes de qualquer reparo.
- Depois que as migrations PostgreSQL forem aplicadas em um ambiente, seus nomes e conteúdos devem permanecer imutáveis; correções futuras devem entrar em uma nova versão e ser testadas tanto em banco vazio quanto em upgrade.
- Novos campos/enum persistentes devem ser adicionados somente por nova migration; migrations aplicadas não devem ser editadas.

# Camada de modelo

## Objetivo do módulo

Agrupar entidades JPA, enums, callbacks de persistência e conversores que representam o modelo persistente.

## Responsabilidades principais

- Definir o grafo `Clinic -> User` e `Clinic -> Patient -> Scheduling`.
- Mapear classes Java para o schema controlado pelo Flyway.
- Representar papéis e estados de agendamento.
- Executar conversões/callbacks associados às entidades.

## Funcionalidades existentes

- Modelos de clínica, usuário, paciente e agendamento.
- Conversão de lista de patologias para JSON.
- Geração de códigos de clínica/paciente.

## Dependências internas e externas

- Internas: subpacotes `clinic`, `user`, `patient`, `scheduling` e `util`; o listener depende de `repository`.
- Externas: JPA/Hibernate, Lombok, Jackson e Spring Security no modelo de usuário.

## Módulos relacionados

`repository`, `service`, `mapper`, `dtos` e migrations.

## Pontos de entrada

Materialização/persistência pelo JPA, criação por mappers/services e serialização indireta em endpoints que ainda retornam entidades.

## Fluxos de entrada

DTO/service -> entidade -> repository/JPA -> MySQL; MySQL -> JPA -> entidade -> mapper ou serialização HTTP.

## Arquivos críticos

- `clinic/Clinic.java`.
- `user/User.java` e `user/UserRole.java`.
- `patient/Patient.java`.
- `scheduling/Scheduling.java`, enum e conversor.
- `util/CodeGeneratorListener.java`.

## Regras confirmadas para evolução do módulo

- Modelo mínimo: `Tenant/Clinic`, `Unit`, `User`, `Patient`, `Professional`, `Service`, `Appointment`, `AppointmentSeries` e `AppointmentEvent`.
- Todo registro de negócio possui `tenantId`, `createdAt` e `updatedAt`, com `createdBy` quando relevante; agendamento possui também `unitId`.
- Relações só podem conectar registros do mesmo tenant.
- Agendamentos e eventos de auditoria nunca são excluídos por cascade ou operação de domínio.
- Datas de negócio usam fuso explícito e agendamentos preservam os snapshots de duração e preço.
- Regras comportamentais pertencem ao domínio/use case e devem ser protegidas por testes e invariantes de persistência, não por callbacks que acessam repositories.

## Observações técnicas e débitos identificados

- O modelo mistura persistência, segurança (`UserDetails`) e callbacks com repositories.
- Entidades são retornadas diretamente em fluxos de usuário.
- Não há isolamento estrutural entre subdomínios.
- Detalhes específicos estão registrados nos READMEs de cada subpacote.

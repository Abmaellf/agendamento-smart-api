# Repositories

## Objetivo do módulo

Definir a porta concreta de persistência Spring Data JPA para clínicas, usuários, pacientes e agendamentos.

## Responsabilidades principais

- CRUD por UUID para todas as entidades.
- Busca de usuário por login e paciente por nome.
- Paginação de pacientes.
- Consulta do maior código de clínica/paciente.
- Query nativa alternativa de clínica por UUID textual.

## Funcionalidades existentes

- `ClinicRepository`, `UserRepository`, `PatientRepository` e `SchedulingRepository`.
- Queries derivadas, JPQL e uma query MySQL nativa.

## Dependências internas e externas

- Internas: todas as entidades persistentes.
- Externas: Spring Data JPA e MySQL para `UUID_TO_BIN` na query nativa.

## Módulos relacionados

`service`, `service/scheduling`, `infra/security`, alguns controllers e `model/util`.

## Pontos de entrada

- Chamadas dos services.
- Chamadas diretas de `AuthenticationController`, `UserController`, `SecurityFilter` e `CodeGeneratorListener`.

## Fluxos de entrada

Service/controller/filtro/listener -> repository proxy -> Hibernate -> datasource MySQL.

## Arquivos críticos

- `ClinicRepository.java`.
- `UserRepository.java`.
- `PatientRepository.java`.
- `SchedulingRepository.java`.

## Regras confirmadas para evolução do módulo

- Toda query de negócio é escopada por `tenantId`; consultas operacionais de agenda também usam `unitId` e intervalo temporal.
- Associações consultadas ou gravadas devem pertencer ao mesmo tenant, mesmo quando o UUID existe.
- CPF de paciente e CPF/CREFITO de profissional são únicos por tenant; e-mail e CPF/CNPJ do contratante são únicos na plataforma.
- Consultas de conflito usam intervalos de início/fim e ignoram somente estados que não ocupam agenda conforme decisão de produto a ser fechada.
- Verificação de capacidade e sobreposição deve permanecer correta sob concorrência; uma sequência ingênua de consulta e `save` não é suficiente.
- Nenhuma operação de repository deve apagar agendamento, série ou evento de auditoria.

## Observações técnicas e débitos identificados

- `ClinicRepository.findByUuid` não possui chamada ativa e usa `UUID_TO_BIN(..., 1)`, enquanto a seed usa `UNHEX(REPLACE(...))`; os formatos podem divergir.
- `SchedulingRepository.findById` e `PatientRepository.findAll(Pageable)` redeclaram operações herdadas.
- `UserRepository.findByLogin` retorna `UserDetails`, exigindo casts em consumidores de domínio.
- `findMaxCode` sustenta uma geração de código sujeita a concorrência.
- `findByName` verifica duplicidade global, sem clínica e sem constraint correspondente no banco.
- A camada é acessada por apresentação, segurança e callbacks de entidade, aumentando o acoplamento transversal.

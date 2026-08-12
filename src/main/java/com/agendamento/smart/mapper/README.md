# Mappers

## Objetivo do módulo

Converter contratos HTTP e entidades JPA por código gerado pelo MapStruct.

## Responsabilidades principais

- Mapear paciente entre request, entidade e response.
- Mapear agendamento entre request, entidade e response.
- Criar usuário a partir do cadastro e da clínica.

## Funcionalidades existentes

- `PatientMapper`: ignora clínica na entrada e extrai `clinic.id` na saída.
- `SchedulingMapper`: ignora ID na entrada e extrai `patient.id` na saída.
- `UserMapper`: ignora ID e combina `RegisterDTO` com `Clinic`.

## Dependências internas e externas

- Internas: DTOs e entidades de paciente, usuário, clínica e agendamento.
- Externas: MapStruct 1.5.5.Final e Spring para registrar implementações geradas.

## Módulos relacionados

`dtos`, `model`, `service`, `service/scheduling` e `controller/user`.

## Pontos de entrada

- Métodos `toEntity`, `toDto` e `toResponse`, invocados por services/controller.
- Implementações geradas em `target/generated-sources/annotations` durante a compilação.

## Fluxos de entrada

DTO -> mapper gerado -> entidade; entidade -> mapper gerado -> DTO. Relações ignoradas na entrada são preenchidas pelo chamador.

## Arquivos críticos

- `PatientMapper.java`.
- `SchedulingMapper.java`.
- `UserMapper.java`.

## Regras confirmadas para evolução do módulo

- Mappers não resolvem tenant, autorização, defaults de estado nem regras de conflito; esses valores vêm do contexto/use case.
- Contratos nunca mapeiam entidade JPA diretamente para respostas de usuário ou segurança.
- Mapeamento de agendamento preserva snapshots de duração/preço e vínculos de unidade, serviço, profissional, série e remarcação.
- Valores legados (`USER`, `ATENDENDO`, `FINALIZADO`) exigem conversão explícita durante a migração para o vocabulário canônico.
- Campos auditáveis controlados pelo servidor não podem ser sobrescritos por requests comuns.

## Observações técnicas e débitos identificados

- `SchedulingMapper` copia status nulo e substitui o default da entidade.
- `UserController` repete campos já mapeados por `UserMapper`.
- `UserMapper.INSTANCE` usa acesso estático não empregado e é redundante com `componentModel="spring"`.
- `UserMapper` mantém imports não utilizados.
- Não há testes dos mapeamentos gerados nem política global para valores nulos.

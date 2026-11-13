# Design

## Context

A API Spring Boot já aplica escopo por clínica em serviços e unidades, usa Flyway com PostgreSQL e devolve `DomainException` pelo advice estruturado. Não há entidade, endpoint ou vínculo de agendamento para pacotes. Esta change não altera a agenda manual e a relação de pacote-agendamento pertence à SPEC 02.

## Goals / Non-Goals

**Goals:**

- Persistir uma definição de pacote por unidade e por serviço da mesma clínica.
- Aplicar autorização de administrador e isolamento de clínica no servidor.
- Expor recursos HTTP que o frontend possa consultar e administrar.

**Non-Goals:**

- Criar contratação, data inicial, ocorrências, conflito, profissional ou horário.
- Criar calendário de funcionamento, feriados, bloqueios ou exclusão física.

## Decisions

### Modelar pacote e dias em tabelas normalizadas

`SERVICE_PACKAGE` terá `id`, `clinic_id`, `unit_id`, `service_id`, `created_by`, nome, descrição anulável, `total_sessions`, `total_price`, `active`, auditoria e versão. `SERVICE_PACKAGE_WEEK_DAY` terá a chave de pacote e a enumeração `MONDAY` a `SUNDAY`, única por pacote. Chaves compostas por clínica seguem o padrão de `APPOINTMENT` e impedem referências entre clínicas.

A frequência é calculada pelo serviço a partir dos dias. JSON foi descartado pois não garante unicidade nem apoia filtros e evolução de cada dia.

### Centralizar autorização e validação no serviço de domínio

O serviço recebe `User`, exige `ADMIN`, consulta unidade e serviço por `clinicId`, valida os valores antes de persistir e devolve DTOs. O controller somente adapta HTTP. `DomainException` usa erros `PACKAGE_FORBIDDEN`, `PACKAGE_NOT_FOUND` e `PACKAGE_IMMUTABLE`; validações Jakarta retornam `VALIDATION_ERROR`.

### Estabelecer contrato REST sem delete

`POST /api/service-packages`, `GET /api/service-packages`, `GET/PATCH /api/service-packages/{id}` e `PATCH /api/service-packages/{id}/status` compõem o recurso. O corpo usa `unitId`, `serviceId`, `totalSessions`, `totalPrice`, `weekDays` e campos descritivos; respostas incluem frequência derivada e auditoria. O projeto não expõe `DELETE`.

### Reservar a trava de imutabilidade para o vínculo da SPEC 02

A entidade desta change não inventa uma série nem ocorrência. O serviço terá uma porta de consulta de uso que inicialmente retorna falso e que a SPEC 02 substituirá por um repositório de vínculo; então PATCH e status devem recusar pacotes usados. Isso evita acoplar o modelo atual a uma contratação inexistente.

## Risks / Trade-offs

- [A change de escopo de unidade está em andamento] → reutilizar `UnitScopeService` e executar a suíte de escopo antes de integrar.
- [Ainda não há vínculo de uso] → a imutabilidade completa depende da SPEC 02; os endpoints já preservam a fronteira para a extensão posterior.
- [Preço enviado como string no frontend existente] → aceitar decimal no DTO e serializar sem ponto flutuante; o adaptador frontend fará a conversão explícita.

## Migration Plan

1. Aplicar a migration nova em banco descartável, validando FKs, checks e unicidade de dias.
2. Entregar serviço, endpoints e testes de isolamento antes do cliente.
3. Manter registros caso haja rollback de aplicação; não derrubar tabelas nem apagar pacotes criados.

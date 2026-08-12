# Controller de agendamentos

## Objetivo do módulo

Expor criação e consulta unitária de agendamentos.

## Responsabilidades principais

- Validar estruturalmente `SchedulingRequest`.
- Delegar operações a `SchedulingService`.
- Retornar `SchedulingResponse`.

## Funcionalidades existentes

- `POST /api/scheduling`: cria agendamento.
- `GET /api/scheduling/{id}`: consulta por UUID.

## Dependências internas e externas

- Internas: DTOs de agendamento e `SchedulingService`.
- Externas: Spring MVC e Jakarta Validation.

## Módulos relacionados

`service/scheduling`, `mapper`, `model/scheduling`, `model/patient`, `repository` e `infra/security`.

## Pontos de entrada

- `SchedulingController.create(SchedulingRequest)`.
- `SchedulingController.findById(UUID)`.

## Fluxos de entrada

- Criação: JSON -> Bean Validation -> service -> paciente -> mapper -> repository -> resposta.
- Consulta: UUID do path -> service -> repository -> mapper -> resposta.

## Arquivos críticos

- `SchedulingController.java`.
- `../../service/scheduling/SchedulingService.java`.

## Regras confirmadas para evolução do módulo

- A API de agenda deve oferecer consulta paginada por intervalo, sempre escopada pelo `tenantId` da sessão e pelo `unitId` autorizado.
- Criação bloqueia, sem exceção, sobreposição do paciente, sobreposição do profissional e capacidade excedida do serviço.
- `ADMIN` e `BASIC` criam, confirmam, iniciam, concluem, registram falta e remarcam; somente `ADMIN` cancela.
- Remarcação exige motivo, marca o original como `REMARCADO` e cria um novo agendamento vinculado. Cancelamento exige motivo e pode atingir uma ocorrência ou “esta e as próximas”.
- Agendamentos não possuem endpoint de exclusão definitiva.
- Os estados canônicos são `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO`; cada transição registra autor e instante.

## Observações técnicas e débitos identificados

- As duas rotas exigem autenticação apenas pela regra padrão; não há autorização por clínica ou papel.
- A regra de segurança específica para `GET /api/scheduling` não casa com `GET /api/scheduling/{id}`.
- Não existem listagem, atualização, cancelamento ou transição de status.
- A criação responde HTTP 200, não 201.
- Não há contrato padronizado para paciente/agendamento inexistente.

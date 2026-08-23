# Service de agendamentos

## Objetivo do módulo

Orquestrar criação e consulta de agendamentos.

## Responsabilidades principais

- Resolver paciente, unidade, serviço e profissional opcional dentro do tenant autenticado.
- Validar data futura, disponibilidade, sobreposições, capacidade, duração e preço.
- Construir e persistir o agendamento com autoria e idempotência.
- Listar por unidade/intervalo ou consultar por UUID e mapear a resposta.

## Funcionalidades existentes

- `create(AppointmentRequest)`.
- `findAll(User, UUID, Instant, Instant)`.
- `findById(UUID)`.

## Dependências internas e externas

- Internas: `AppointmentRepository`, repositories dos recursos relacionados, entidades e DTOs.
- Externas: Spring Service e Lombok.

## Módulos relacionados

`controller/appointment`, `dtos/appointment`, `model/appointment`, `model/patient` e `repository`.

## Pontos de entrada

- `POST /api/appointments` chama `create`.
- `GET /api/appointments` chama `findAll`.
- `GET /api/appointments/{id}` chama `findById`.

## Fluxos de entrada

- Criação: request -> resolução dos recursos do tenant -> associação -> save -> resposta.
- Consulta: filtros ou UUID -> repository -> conversão manual de resposta.

## Arquivos críticos

- `AppointmentService.java`.

## Regras confirmadas para evolução do módulo

- Criar agenda valida tenant/unidade, paciente, serviço, profissional opcional, duração/preço e início com fuso explícito em uma transação.
- A criação bloqueia sem exceção sobreposição do paciente, sobreposição do profissional e capacidade excedida, inclusive sob concorrência.
- Duração e preço do serviço são copiados para o agendamento; apenas `ADMIN` pode sobrescrevê-los.
- Profissional pode ser atribuído depois, mas é obrigatório para iniciar ou concluir.
- Transições seguem o grafo canônico e geram `AppointmentEvent` com autor e instante.
- Remarcação exige motivo, finaliza o original como `REMARCADO` e cria um novo agendamento vinculado; ambos os perfis podem executar.
- Cancelamento exige motivo, só pode ser executado por `ADMIN` e, em séries, admite uma ocorrência ou “esta e as próximas”.
- Agendamentos, séries e eventos nunca são excluídos. Recorrência e comandos em série precisam de atomicidade conforme decisão pendente da entrevista.

## Observações técnicas e débitos identificados

- A criação serializa operações por clínica para proteger conflitos e capacidade, o que limita paralelismo dentro do mesmo tenant.
- Ainda não há comandos de transição de status, remarcação, cancelamento ou recorrência.
- O subpacote separa agendamento dos demais services, mas não constitui módulo isolado: depende diretamente de paciente e repositories compartilhados.

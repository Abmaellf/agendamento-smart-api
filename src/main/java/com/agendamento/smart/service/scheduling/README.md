# Service de agendamentos

## Objetivo do módulo

Orquestrar criação e consulta de agendamentos.

## Responsabilidades principais

- Resolver o paciente informado no request.
- Mapear request para entidade e associar o paciente.
- Persistir agendamento.
- Consultar por UUID e mapear a resposta.

## Funcionalidades existentes

- `create(SchedulingRequest)`.
- `findById(UUID)`.

## Dependências internas e externas

- Internas: `SchedulingRepository`, `PatientRepository`, `SchedulingMapper`, entidades e DTOs.
- Externas: Spring Service e Lombok.

## Módulos relacionados

`controller/scheduling`, `dtos/agendamento`, `model/scheduling`, `model/patient`, `mapper` e `repository`.

## Pontos de entrada

- `POST /api/scheduling` chama `create`.
- `GET /api/scheduling/{id}` chama `findById`.

## Fluxos de entrada

- Criação: request -> busca de paciente -> mapper -> associação -> save -> mapper de resposta.
- Consulta: UUID -> repository -> mapper de resposta.

## Arquivos críticos

- `SchedulingService.java`.

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

- Não há validação de clínica, papel, conflito de horário, data futura ou transição de status.
- O service aceita qualquer paciente existente, independentemente da clínica do usuário.
- Não há `@Transactional` explícito na operação composta de busca e gravação.
- Exceções de ausência são `IllegalArgumentException`, sem tratamento HTTP padronizado.
- A ausência de status no request pode gerar entidade com status nulo por comportamento do mapper.
- O subpacote separa agendamento dos demais services, mas não constitui módulo isolado: depende diretamente de paciente e repositories compartilhados.

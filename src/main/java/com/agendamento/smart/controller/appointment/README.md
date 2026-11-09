# Controller de agendamentos

## Objetivo do módulo

Expor criação, listagem por intervalo e consulta unitária de agendamentos.

## Responsabilidades principais

- Validar estruturalmente `AppointmentRequest`.
- Delegar operações a `AppointmentService`.
- Retornar `AppointmentResponse`.

## Funcionalidades existentes

- `POST /api/appointments`: cria agendamento.
- `GET /api/appointments`: lista por clínica e intervalo, com `unitId` opcional.
- `GET /api/appointments/{id}`: consulta por UUID.

## Dependências internas e externas

- Internas: DTOs de agendamento e `AppointmentService`.
- Externas: Spring MVC e Jakarta Validation.

## Módulos relacionados

`service/appointment`, `model/appointment`, `model/patient`, `repository` e `infra/security`.

## Pontos de entrada

- `AppointmentController.create(AppointmentRequest)`.
- `AppointmentController.findAll(...)`.
- `AppointmentController.findById(UUID)`.

## Fluxos de entrada

- Criação: JSON -> Bean Validation -> service -> recursos do tenant -> repository -> resposta.
- Consulta: filtros ou UUID do path -> service -> repository -> resposta.

## Arquivos críticos

- `AppointmentController.java`.
- `../../service/appointment/AppointmentService.java`.

## Regras implementadas

- A clínica vem do principal autenticado; IDs de outro tenant não autorizam acesso.
- `from` e `to` são obrigatórios, formam `[from,to)` e exigem `from < to`.
- Sem `unitId`, a listagem abrange unidades legíveis da clínica; com ele, valida e filtra uma unidade.
- Clínica/unidade fora de `ATIVO` bloqueia criação; somente `INATIVO` bloqueia leitura histórica.
- Paciente deve pertencer à unidade e profissional opcional, quando informado, também.

## Regras confirmadas para evolução do módulo

- A API de agenda poderá receber paginação em change própria se o volume exigir.
- Criação bloqueia, sem exceção, sobreposição do paciente, sobreposição do profissional e capacidade excedida do serviço.
- `ADMIN` e `BASIC` criam, confirmam, iniciam, concluem, registram falta e remarcam; somente `ADMIN` cancela.
- Remarcação exige motivo, marca o original como `REMARCADO` e cria um novo agendamento vinculado. Cancelamento exige motivo e pode atingir uma ocorrência ou “esta e as próximas”.
- Agendamentos não possuem endpoint de exclusão definitiva.
- Os estados canônicos são `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO`; cada transição registra autor e instante.

## Observações técnicas e débitos identificados

- As três rotas exigem autenticação; o service restringe os recursos ao tenant do usuário autenticado.
- A regra específica para `GET /api/appointments` cobre a listagem; a consulta por ID permanece autenticada pela regra padrão.
- Não existem atualização, cancelamento ou transição de status.
- A criação responde HTTP 201.
- Ausências e conflitos usam o contrato central de erros de domínio.
- A matriz completa de status está em `docs/API_APPOINTMENTS.md` na raiz do projeto.

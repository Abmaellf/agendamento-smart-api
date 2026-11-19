# Controller de agendamentos

## Objetivo do módulo

Expor criação, listagem por intervalo, consulta unitária e transições iniciais de status dos agendamentos.

## Responsabilidades principais

- Validar estruturalmente `AppointmentRequest`.
- Delegar operações a `AppointmentService`.
- Retornar `AppointmentResponse`.

## Funcionalidades existentes

- `POST /api/appointments`: cria agendamento.
- `GET /api/appointments`: lista por clínica e intervalo, com `unitId` opcional.
- `GET /api/appointments/{id}`: consulta por UUID.
- `PATCH /api/appointments/{id}/status`: inicia ou cancela um agendamento em `AGENDADO`.

## Dependências internas e externas

- Internas: DTOs de agendamento e `AppointmentService`.
- Externas: Spring MVC e Jakarta Validation.

## Módulos relacionados

`service/appointment`, `model/appointment`, `model/patient`, `repository` e `infra/security`.

## Pontos de entrada

- `AppointmentController.create(AppointmentRequest)`.
- `AppointmentController.findAll(...)`.
- `AppointmentController.findById(UUID)`.
- `AppointmentController.updateStatus(UUID, AppointmentStatusRequest, User)`.

## Fluxos de entrada

- Criação: JSON -> Bean Validation -> service -> recursos do tenant -> repository -> resposta.
- Consulta: filtros ou UUID do path -> service -> repository -> resposta.
- Status: comando limitado -> lock por clínica -> validação de unidade, ciclo de vida e prazo -> repository -> resposta.

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
- `ADMIN` e `BASIC` podem iniciar ou cancelar, desde que tenham acesso à unidade; comandos de conclusão, falta manual e remarcação permanecem fora desta change.
- Remarcação exige motivo, marca o original como `REMARCADO` e cria um novo agendamento vinculado. Cancelamento exige motivo e pode atingir uma ocorrência ou “esta e as próximas”.
- Agendamentos não possuem endpoint de exclusão definitiva.
- O enum persistido nesta API é `AGENDADO`, `ATENDENDO`, `CANCELADO`, `FALTA` e `FINALIZADO`; a ampliação para o workflow canônico completo permanece fora desta change.

## Observações técnicas e débitos identificados

- As três rotas exigem autenticação; o service restringe os recursos ao tenant do usuário autenticado.
- A regra específica para `GET /api/appointments` cobre a listagem; a consulta por ID permanece autenticada pela regra padrão.
- A atualização aceita somente `ATENDENDO` e `CANCELADO` a partir de `AGENDADO`.
- `FALTA` é atribuído automaticamente após dez minutos do início sem `ATENDENDO`.
- A criação responde HTTP 201.
- Ausências e conflitos usam o contrato central de erros de domínio.
- A matriz completa de status está em `docs/API_APPOINTMENTS.md` na raiz do projeto.

# Requisitos do MVP

## Controle do documento

Este documento consolida as regras fornecidas para o backend e as decisões da entrevista de produto registrada no frontend `agendamentos-smart`. Ele descreve o comportamento esperado, não o estado já implementado. Para o estado executável atual, consulte [Objetivo do sistema](OBJETIVO_DO_SISTEMA.md) e [Arquitetura do Sistema](ARQUITETURA_DO_SISTEMA.md).

Questões ainda não respondidas ficam em [Entrevista de descoberta](ENTREVISTA_DE_DESCOBERTA.md). Não se deve preencher uma lacuna de produto por conveniência técnica.

## Alvo aprovado e estado de entrega

As regras das seções seguintes são o **alvo aprovado**. Este documento não é
evidência de que uma regra foi entregue. A evidência vem de migrations, código,
testes executados e specs principais sincronizadas.

Em 2026-11-08, a change ativa `estrutura-clinica-matriz-vinculos` possui como
comportamento implementado e verificado no working tree:

- V010 com status/tipos, unicidade de `ADMIN`/MATRIZ e vínculos por unidade;
- onboarding transacional de clínica, MATRIZ, equipe e contas iniciais;
- cadastro posterior de usuário/profissional e paciente com vínculos validados;
- criação e leitura de appointments com isolamento, status e listagem por
  clínica ou unidade;
- `./mvnw clean verify` executado em PostgreSQL descartável com 34 testes e
  nenhuma falha ou erro.

A sincronização das specs principais e o arquivamento permanecem como etapa de
workflow posterior à revisão da change. O progresso observável está em
`openspec/changes/estrutura-clinica-matriz-vinculos/tasks.md`.

## Produto, tenant e unidade

O produto é um SaaS de agendamentos para fisioterapia, Pilates e atividades relacionadas. O contratante pode ser pessoa física ou jurídica. O primeiro cadastro cria, atomicamente e já em estado `ATIVO`, a clínica, sua unidade `MATRIZ` `ATIVO`, um profissional/usuário `ATENDENTE` e um profissional/usuário `ADMINISTRATIVO`. O administrativo recebe o único papel `ADMIN` da clínica; o atendente recebe `USER`. Validação de e-mail e bloqueio inicial não fazem parte desta fase.

No contrato canônico, a clínica é o `Tenant`: a conta do cliente e a fronteira obrigatória de segurança. `Unit` é uma entidade filha. No primeiro fluxo existe apenas a MATRIZ, mas o modelo admite `MATRIZ` e `FILIAL`, e todo agendamento possui `unitId`.

```text
Tenant/Clinic
├── Unit MATRIZ
│   ├── Professionals exclusivos da Unit
│   └── Patients vinculados à Unit
├── Users associados somente à Clinic
├── Patients vinculados a uma ou mais Units da mesma Clinic
├── Professionals vinculados a exatamente uma Unit
├── Services
└── Appointments vinculados à Unit
```

Cada paciente pertence a uma clínica e a uma ou mais unidades dessa clínica; não pode atravessar clínicas. Cada profissional pertence a uma clínica e a exatamente uma unidade da mesma clínica nesta fase. A criação de FILIAL, o papel `GERENTE` e o modal de seleção de unidade ficam para uma etapa posterior.

Todo registro de negócio deve possuir `tenantId`, `createdAt` e `updatedAt`; comandos auditáveis também guardam `createdBy` ou o autor equivalente. O tenant efetivo deve ser derivado da sessão autenticada e não aceito como autoridade a partir de um campo editável do request.

## Vocabulário canônico e legado

| Conceito canônico | Nome atual no backend | Direção aprovada |
|---|---|---|
| `Tenant`/clínica-conta | `Clinic` | `Clinic` passa a representar explicitamente o tenant; unidade física será `Unit` |
| `Appointment` | `Appointment` | vocabulário adotado de forma consistente nos contratos e no domínio |
| `BASIC` | `USER` | perfil operacional básico, sem permissão de cancelamento |
| `EM_ATENDIMENTO` | `ATENDENDO` | usar o estado canônico |
| `CONCLUIDO` | `FINALIZADO` | usar o estado canônico |

O nome anterior permanece apenas no histórico imutável do Flyway (`V008__create_table_scheduling.sql` e `R__008_seed_scheduling.sql`); a `V009` faz a transição compatível para `APPOINTMENT`. Código e contratos correntes usam `Appointment`.

## Perfis e permissões

Os papéis persistidos nesta fase são `ADMIN` e `USER`. `BASIC` permanece como vocabulário canônico futuro para o papel operacional hoje persistido como `USER`. Um futuro usuário master da plataforma não é um papel operacional da clínica.

| Operação | `ADMIN` | `BASIC` |
|---|---:|---:|
| Consultar agenda e cadastros permitidos | Sim | Sim |
| Cadastrar pacientes e profissionais | Sim | Sim |
| Criar, confirmar, iniciar, concluir e registrar falta | Sim | Sim |
| Remarcar agendamento | Sim | Sim |
| Cancelar agendamento com motivo | Sim | Não |
| Excluir agendamento | Não | Não |
| Criar usuário `USER` | Sim | Sim |
| Criar, promover ou substituir o `ADMIN` único | Não nesta fase | Não |
| Sobrescrever duração ou preço padrão | Sim | Não |
| Administrar configurações restritas | Sim | Não |

A API deve validar tenant e permissão em toda operação. Controles do frontend são apenas conveniência de interface.

No cadastro inicial, login e senha das contas ATENDENTE e ADMINISTRATIVO são informados na própria requisição. A política de troca obrigatória, redefinição e recuperação de senha fica fora desta change.

## Cadastros mínimos

### Tenant e unidade

- CPF ou CNPJ do contratante é único em toda a plataforma;
- e-mail de acesso é único em toda a plataforma;
- cada usuário pertence a exatamente uma clínica;
- existe exatamente um `ADMIN` por clínica e não há substituição nesta fase;
- o cadastro cria somente uma unidade `MATRIZ`, já `ATIVO`, e a clínica também nasce `ATIVO`;
- `StatusClinic` e `StatusUnit` admitem `ATIVO`, `INATIVO` e `BLOQUEADA`;
- `TypeUnit` admite `MATRIZ` e `FILIAL`, mas não há endpoint de FILIAL ou de mudança de status nesta fase.

### Paciente

- nome, CPF e telefone são obrigatórios;
- CPF é único dentro do mesmo tenant;
- pertence a exatamente uma clínica e a uma ou mais unidades dessa clínica;
- pode estar em várias unidades da mesma clínica, nunca em clínicas diferentes;
- paciente não contém prontuário, diagnóstico, evolução nem documentos clínicos no MVP;
- regras de edição, inativação e anonimização ainda dependem da entrevista.

### Profissional

- nome, CPF, CREFITO, telefone, especialidades, serviços executados e dias disponíveis são dados confirmados;
- CPF e CREFITO são únicos dentro do mesmo tenant;
- profissional e usuário são entidades diferentes e podem ser vinculados quando o profissional também acessa o sistema;
- profissional pertence a exatamente uma unidade da própria clínica e não atua em múltiplas unidades nesta fase;
- a MATRIZ nasce com os tipos `ATENDENTE` e `ADMINISTRATIVO`, ambos vinculados a contas;
- faixas de horário, intervalos, férias, salas e equipamentos ficam fora do MVP.

### Serviço

- nome, duração, preço padrão e capacidade são obrigatórios;
- capacidade deve estar entre 1 e 10;
- usuário básico usa duração e preço padrão do serviço;
- somente administrador pode sobrescrever duração ou preço no agendamento.

## Agendamento

Cada agendamento representa exatamente um paciente, inclusive em atividades coletivas. São obrigatórios: paciente, unidade, serviço/modalidade, início, duração e preço aplicado. O profissional pode ser atribuído depois da criação, mas passa a ser obrigatório antes de iniciar ou concluir o atendimento.

Na criação, o paciente deve estar vinculado à unidade escolhida. O profissional
permanece opcional; quando informado, deve pertencer à mesma unidade. A futura
obrigatoriedade antes de `EM_ATENDIMENTO` não faz parte desta change.

### Status da clínica/unidade e agenda

O login continua permitido nos três estados da clínica e da unidade. As regras
da agenda são:

| Status efetivo | Login | Criar appointment | Ler histórico |
|---|---:|---:|---:|
| clínica e unidade `ATIVO` | Sim | Sim | Sim |
| clínica ou unidade `INATIVO` | Sim | Não | Não |
| clínica ou unidade `BLOQUEADA`, sem `INATIVO` | Sim | Não | Sim |

`INATIVO` tem precedência sobre `BLOQUEADA` para leitura: se qualquer parte
relevante estiver inativa, a consulta é negada. Em uma listagem que abranja
várias unidades, unidades `INATIVO` são excluídas; filtro explícito ou detalhe
de unidade inativa responde com proibição.

O agendamento preserva cópias da duração e do preço vigentes na criação. Mudanças posteriores no serviço não podem alterar o histórico. Datas devem ser armazenadas com informação explícita de fuso horário; a representação exata será fechada na entrevista.

### Capacidade e conflitos

O sistema deve bloquear para todos os perfis, sem exceção administrativa:

- sobreposição para o mesmo paciente;
- sobreposição para o mesmo profissional;
- capacidade excedida do serviço.

A sobreposição usa início e duração. A unidade e a semântica precisa da capacidade durante intervalos parcialmente sobrepostos ainda serão detalhadas na entrevista. Essas verificações devem ser transacionais e seguras sob requisições concorrentes.

### Estados do primeiro MVP

Estados canônicos: `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO`.

| Origem | Destinos permitidos |
|---|---|
| `AGENDADO` | `CONFIRMADO`, `EM_ATENDIMENTO`, `CANCELADO`, `FALTA` ou `REMARCADO` |
| `CONFIRMADO` | `EM_ATENDIMENTO`, `CANCELADO`, `FALTA` ou `REMARCADO` |
| `EM_ATENDIMENTO` | `CONCLUIDO` |
| estados finais | `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO` não recebem transição comum |

Confirmação, início, conclusão e falta são ações manuais no primeiro MVP. Toda transição registra autor e instante.

### Recorrência, remarcação e cancelamento

- recorrência cria ocorrências independentes vinculadas a `AppointmentSeries`;
- alterar ou remarcar uma ocorrência afeta somente o dia escolhido;
- remarcação transforma a ocorrência original em `REMARCADO` e cria um novo agendamento vinculado;
- usuários `ADMIN` e `BASIC` podem remarcar;
- remarcação registra autor, instante, motivo e relacionamento com o novo agendamento;
- somente `ADMIN` cancela, sempre informando motivo;
- em série recorrente, o cancelamento pode atingir apenas uma ocorrência ou “esta e as próximas”;
- agendamentos nunca são excluídos definitivamente.

Toda mudança relevante deve produzir um `AppointmentEvent` imutável. A forma exata de recorrência e o tratamento de ocorrências já remarcadas no cancelamento em massa ainda dependem da entrevista.

## Entidades mínimas

- `Tenant`/`Clinic`;
- `Unit`;
- `User`;
- `Patient`;
- `Professional`;
- `Service`;
- `Appointment`;
- `AppointmentSeries`;
- `AppointmentEvent`.

Associações devem ser validadas dentro do mesmo tenant. `Appointment` também pertence a uma unidade e guarda snapshots de duração e preço. Exclusões em cascata que eliminem histórico de agendamento são incompatíveis com estas regras.

## Contrato HTTP e persistência

- contratos expõem DTOs, nunca entidades JPA ou hashes de senha;
- IDs enviados pelo cliente selecionam recursos, mas tenant e autorização são derivados da sessão;
- erros de validação, conflito, capacidade, estado inválido, ausência e proibição devem possuir códigos HTTP e corpo estáveis;
- listagens devem ser paginadas e filtradas pelo tenant; agenda também usa unidade e intervalo temporal;
- escritas compostas, recorrência, remarcação e cancelamento em série são transacionais;
- migrations Flyway são a fonte de verdade do schema e migrations aplicadas não são reescritas;
- índices, constraints e estratégia de concorrência devem sustentar as invariantes, não apenas validações de interface.

## Fora do primeiro MVP

- confirmação automática quatro horas antes;
- falta automática ao terminar a duração sem início;
- conclusão automática ao terminar a duração de um atendimento iniciado;
- operação efetiva de múltiplas unidades;
- criação de FILIAL, modal de seleção de unidade e papel `GERENTE`;
- substituição do `ADMIN` único e operações de alteração de status/MATRIZ;
- recuperação de senha por e-mail;
- pagamentos, pacotes de sessões e controle financeiro;
- prontuário, diagnóstico, evolução e documentos clínicos;
- salas, equipamentos, faixas intradiárias, intervalos e férias;
- WhatsApp, SMS, lembretes e demais notificações;
- exclusão definitiva de agendamento.

## Critério de implementação

Uma funcionalidade só pode ser declarada implementada quando possuir autorização e isolamento por tenant, validação de entrada, persistência/migration quando aplicável, contrato HTTP estável, auditoria exigida, tratamento de concorrência relevante e testes proporcionais às regras de negócio.

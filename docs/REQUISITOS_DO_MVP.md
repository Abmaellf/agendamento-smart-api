# Requisitos do MVP

## Controle do documento

Este documento consolida as regras fornecidas para o backend e as decisões da entrevista de produto registrada no frontend `agendamentos-smart`. Ele descreve o comportamento esperado, não o estado já implementado. Para o estado executável atual, consulte [Objetivo do sistema](OBJETIVO_DO_SISTEMA.md) e [Arquitetura do Sistema](ARQUITETURA_DO_SISTEMA.md).

Questões ainda não respondidas ficam em [Entrevista de descoberta](ENTREVISTA_DE_DESCOBERTA.md). Não se deve preencher uma lacuna de produto por conveniência técnica.

## Produto, tenant e unidade

O produto é um SaaS de agendamentos para fisioterapia, Pilates e atividades relacionadas. O contratante pode ser pessoa física ou jurídica. O primeiro cadastro cria a conta/clínica, uma unidade padrão e o primeiro usuário administrador; a conta permanece bloqueada até a validação do e-mail.

No contrato canônico, a clínica é o `Tenant`: a conta do cliente e a fronteira obrigatória de segurança. `Unit` é uma entidade filha. No primeiro MVP existe apenas a unidade padrão em operação, mas todo agendamento já deve possuir `unitId`.

```text
Tenant/Clinic
├── Unit padrão
├── Users
├── Patients compartilhados
├── Professionals compartilhados
├── Services
└── Appointments vinculados à Unit
```

Pacientes e profissionais pertencem ao tenant e são compartilhados entre suas futuras unidades. A operação efetiva de múltiplas unidades para clientes pessoa jurídica fica para uma etapa posterior.

Todo registro de negócio deve possuir `tenantId`, `createdAt` e `updatedAt`; comandos auditáveis também guardam `createdBy` ou o autor equivalente. O tenant efetivo deve ser derivado da sessão autenticada e não aceito como autoridade a partir de um campo editável do request.

## Vocabulário canônico e legado

| Conceito canônico | Nome atual no backend | Direção aprovada |
|---|---|---|
| `Tenant`/clínica-conta | `Clinic` | `Clinic` passa a representar explicitamente o tenant; unidade física será `Unit` |
| `Appointment` | `Scheduling` | contratos novos usam o conceito de agendamento/appointment de forma consistente |
| `BASIC` | `USER` | perfil operacional básico, sem permissão de cancelamento |
| `EM_ATENDIMENTO` | `ATENDENDO` | usar o estado canônico |
| `CONCLUIDO` | `FINALIZADO` | usar o estado canônico |

Os nomes legados continuam sendo descrição do código atual até uma refatoração acompanhada de migration e compatibilidade de contrato.

## Perfis e permissões

Existem os perfis de tenant `ADMIN` e `BASIC`. Um futuro usuário master da plataforma não é um papel operacional da clínica.

| Operação | `ADMIN` | `BASIC` |
|---|---:|---:|
| Consultar agenda e cadastros permitidos | Sim | Sim |
| Cadastrar pacientes e profissionais | Sim | Sim |
| Criar, confirmar, iniciar, concluir e registrar falta | Sim | Sim |
| Remarcar agendamento | Sim | Sim |
| Cancelar agendamento com motivo | Sim | Não |
| Excluir agendamento | Não | Não |
| Criar usuário básico | Sim | Sim |
| Criar ou promover administrador | Sim | Não |
| Sobrescrever duração ou preço padrão | Sim | Não |
| Administrar configurações restritas | Sim | Não |

A API deve validar tenant e permissão em toda operação. Controles do frontend são apenas conveniência de interface.

O administrador define uma senha temporária para usuários criados. Ela deve ser trocada no primeiro acesso e não pode ser reutilizada após a troca. No MVP, somente o administrador redefine senha; recuperação por e-mail está fora do escopo.

## Cadastros mínimos

### Tenant e unidade

- CPF ou CNPJ do contratante é único em toda a plataforma;
- e-mail de acesso é único em toda a plataforma;
- o cadastro cria uma unidade padrão;
- dados exatos, estados de ativação e ciclo de manutenção ainda dependem da entrevista.

### Paciente

- nome, CPF e telefone são obrigatórios;
- CPF é único dentro do mesmo tenant;
- paciente não contém prontuário, diagnóstico, evolução nem documentos clínicos no MVP;
- regras de edição, inativação e anonimização ainda dependem da entrevista.

### Profissional

- nome, CPF, CREFITO, telefone, especialidades, serviços executados e dias disponíveis são dados confirmados;
- CPF e CREFITO são únicos dentro do mesmo tenant;
- profissional e usuário são entidades diferentes e podem ser vinculados quando o profissional também acessa o sistema;
- faixas de horário, intervalos, férias, salas e equipamentos ficam fora do MVP.

### Serviço

- nome, duração, preço padrão e capacidade são obrigatórios;
- capacidade deve estar entre 1 e 10;
- usuário básico usa duração e preço padrão do serviço;
- somente administrador pode sobrescrever duração ou preço no agendamento.

## Agendamento

Cada agendamento representa exatamente um paciente, inclusive em atividades coletivas. São obrigatórios: paciente, unidade, serviço/modalidade, início, duração e preço aplicado. O profissional pode ser atribuído depois da criação, mas passa a ser obrigatório antes de iniciar ou concluir o atendimento.

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
- recuperação de senha por e-mail;
- pagamentos, pacotes de sessões e controle financeiro;
- prontuário, diagnóstico, evolução e documentos clínicos;
- salas, equipamentos, faixas intradiárias, intervalos e férias;
- WhatsApp, SMS, lembretes e demais notificações;
- exclusão definitiva de agendamento.

## Critério de implementação

Uma funcionalidade só pode ser declarada implementada quando possuir autorização e isolamento por tenant, validação de entrada, persistência/migration quando aplicável, contrato HTTP estável, auditoria exigida, tratamento de concorrência relevante e testes proporcionais às regras de negócio.

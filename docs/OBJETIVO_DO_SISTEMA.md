# Objetivo do sistema

## Identificação

| Campo | Valor |
|---|---|
| Sistema | `agendamento-smart-api` |
| Data de referência | 8 de novembro de 2026 |
| Evidências | Controllers, services, entidades, repositories, migrations, segurança, `README.md` e configuração operacional |

Este documento descreve a capacidade implementada. O estado-alvo confirmado está em [Requisitos do MVP](REQUISITOS_DO_MVP.md), e as decisões ainda abertas estão em [Entrevista de descoberta](ENTREVISTA_DE_DESCOBERTA.md). Uma regra de produto confirmada, mas ausente do código, continua sendo descrita aqui como não implementada.

## Propósito principal

O sistema fornece uma API HTTP para autenticar usuários vinculados a clínicas, cadastrar e consultar pacientes e criar/consultar agendamentos desses pacientes. Ele centraliza esses dados em PostgreSQL e protege parte das operações com JWT e papéis `ADMIN`/`USER`.

O README existente descreve uso em áreas médicas e clínicas de Psicologia e Fisioterapia. O código atual é genérico: não implementa prontuário, especialidade clínica ou regra própria de uma dessas áreas.

## Problemas que o sistema resolve atualmente

- Autenticação de usuários por login e senha BCrypt.
- Emissão e validação de token JWT para requisições stateless.
- Onboarding atômico de clínica, MATRIZ, equipe operacional e contas iniciais.
- Associação obrigatória de usuários a uma clínica, profissionais a uma unidade e pacientes a uma ou várias unidades da mesma clínica.
- Cadastro posterior de usuário, com escolha obrigatória sobre perfil profissional.
- Cadastro de paciente por clínica autenticada e listagem paginada legada.
- Criação de agendamento com status, vínculos, conflitos, capacidade e tenant validados.
- Armazenamento de uma lista de patologias em `jsonb` por agendamento.
- Consulta de agendamento por UUID ou intervalo, por clínica e unidade opcional.
- Versionamento do schema com Flyway.
- Consulta da versão da aplicação para diagnóstico de build.

O código ainda não resolve faixas intradiárias, férias, recursos/salas,
notificações, pagamento, histórico clínico ou transições completas do ciclo do
appointment.

## Atores envolvidos

| Ator | Capacidades efetivamente observadas |
|---|---|
| Cliente não autenticado | Efetuar login, cadastrar inicialmente uma clínica e acessar a listagem pública legada de pacientes |
| Usuário `USER` autenticado | Consultar perfil, criar/consultar appointments e cadastrar somente outro `USER` em sua clínica |
| Usuário `ADMIN` autenticado | Capacidades de `USER`, criação de pacientes e sobrescrita permitida de duração/preço |
| Clínica | Tenant com status explícito, onboarding público e uma MATRIZ inicial |
| Paciente | Entidade de uma clínica associada a uma ou várias unidades dela; não é principal autenticável |
| Operação/implantação | Configura banco, segredo JWT, migrations, porta e versão; pode executar via Maven ou Docker Compose |

A clínica representa o tenant e já delimita onboarding, cadastros posteriores e
agenda. A listagem pública legada de pacientes permanece uma exceção explícita.

## Principais fluxos de negócio

### 1. Login

Entrada: login e senha.

1. O usuário é localizado por login.
2. Spring Security compara a senha com o hash persistido.
3. A API emite JWT com o login como subject.
4. O token é devolvido no corpo e em cookie HTTP-only.
5. A resposta também contém a entidade de usuário atual.

Resultado: o cliente passa a autenticar requisições por Bearer token ou cookie `jwt`.

### 2. Identificação do usuário atual

Entrada: JWT válido.

1. O filtro recupera o token e localiza o usuário pelo login.
2. O controller `/auth/me` pede ao `UserService` o principal atual.
3. O service recarrega o usuário por UUID.

Resultado: a entidade de usuário é devolvida ao cliente.

### 3. Cadastro inicial e posterior de usuário

`POST /auth/register/clinic` cria atomicamente clínica/MATRIZ ativas, contas e
profissionais ATENDENTE/ADMINISTRATIVO. O administrativo é o único `ADMIN`.

No cadastro posterior, o `clinicId` do path deve coincidir com a clínica do
principal, o papel aceito é somente `USER` e `isProfessional` é obrigatório.
Quando verdadeiro, a conta e o profissional exclusivo da MATRIZ são persistidos
juntos. A resposta é HTTP 200 sem corpo e a senha fica codificada com BCrypt.

### 4. Cadastro de paciente

Entrada: nome e lista não vazia de unidades.

1. A rota exige papel `ADMIN`.
2. A clínica é derivada do principal autenticado.
3. Todas as unidades são validadas na mesma clínica.
4. O nome é transformado para maiúsculas e mantém a unicidade global legada.
5. Paciente e associações de unidade são persistidos na mesma transação.

Resultado: UUID, nome, UUID da clínica e `unitIds`.

### 5. Listagem de pacientes

Entrada: parâmetros `Pageable` reconhecidos pelo Spring Data.

1. Todos os pacientes são consultados sem filtro por clínica.
2. As entidades são convertidas em DTOs.
3. A resposta inclui conteúdo, página, tamanho, totais e indicador de última página.

Resultado: página de pacientes. A rota é pública na configuração atual.

### 6. Criação de agendamento

Entrada: paciente, unidade, serviço, profissional opcional, instante inicial,
duração e preço.

1. A clínica vem do usuário autenticado e é bloqueada durante a escrita.
2. Clínica e unidade precisam estar `ATIVO`.
3. Paciente precisa pertencer à unidade; profissional, quando informado, também.
4. O service valida futuro, disponibilidade semanal, sobreposições, capacidade e permissão para sobrescrever duração/preço.
5. O appointment nasce `AGENDADO`, com autoria, fuso e snapshots persistidos.

### 7. Consulta de agendamento

Por UUID, o repository combina ID e clínica autenticada. Na listagem, `from` e
`to` são obrigatórios, `unitId` é opcional e o resultado usa `[from,to)` em
ordem crescente. Clínica ou unidade `INATIVO` impede leitura explícita;
`BLOQUEADA` permite histórico e a listagem ampla exclui unidades inativas.

## Funcionalidades centrais e estágio

| Área | Implementado | Não implementado no código analisado |
|---|---|---|
| Autenticação | Login, JWT, cookie, Bearer, papéis | Logout, refresh/revogação, recuperação de senha, bloqueio de conta |
| Usuários | Cadastro posterior restrito a `USER`, perfil profissional opcional, listagem e usuário atual | Atualização, exclusão, convite |
| Clínicas | Onboarding com MATRIZ/equipe, status e invariantes persistentes | Consulta, atualização, FILIAL e transição de status |
| Pacientes | Cadastro isolado com vínculos de unidades e listagem paginada legada | Busca por ID, atualização, exclusão e correção da listagem pública |
| Agendamentos | Criação, consulta por ID, listagem por clínica/unidade, conflitos e capacidade | Alteração, cancelamento, finalização e recorrência |
| Operação | Flyway, build info, Dockerfile/Compose | Health check HTTP, métricas, tracing e documentação OpenAPI |

## Visão de produto

O produto implementado é uma API multi-tenant inicial de gestão de
agendamentos. A clínica delimita cadastros e agenda; o onboarding cria a MATRIZ
e a equipe operacional, profissionais pertencem a uma unidade e pacientes
podem pertencer a várias unidades da mesma clínica.

O enum Java e o check do schema PostgreSQL aceitam `AGENDADO`, `ATENDENDO`, `CANCELADO` e `FINALIZADO`; porém não existem endpoints nem regras de transição. Portanto, o ciclo completo não deve ser tratado como funcionalidade existente.

O ciclo-alvo confirmado usa `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`,
`CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO`, com ações manuais no primeiro
MVP. Capacidade e bloqueios de sobreposição já existem na criação; recorrência,
remarcação vinculada e cancelamento auditável ainda não estão implementados.

## Contexto operacional

- Aplicação Java 21/Spring Boot executada como JAR único.
- Porta HTTP fixa `8080` na configuração da aplicação.
- Banco PostgreSQL 18.1 no ambiente anteriormente validado; o Compose atual usa a tag flutuante `bitnami/postgresql:latest`, publica por padrão `127.0.0.1:5432` e persiste em `postgresql_data`. Ambientes persistentes devem fixar tag imutável ou digest homologado.
- Schema gerenciado pelo Flyway `11.14.1` na inicialização, com `baseline-on-migrate=false` e `out-of-order=false`.
- Identificadores são persistidos como `UUID` nativo, patologias como `jsonb` e instantes absolutos de agendamento/auditoria como `timestamptz`.
- Configuração de conexão por `AGENDA_URL`, `AGENDA_DB_USER` e `AGENDA_DB_PASSWORD`.
- Profiles Spring `test`, `homo` e `prod`, selecionados no runtime sobre um único JAR.
- `homo` e `prod` carregam somente `classpath:db/migration`; `test` e o Compose também incluem as seeds de desenvolvimento de `classpath:db/seed/dev`.
- Assinatura JWT por `JWT_SECRET` obrigatório, sem segredo default no repositório.
- Origens CORS globais: frontend Vercel do projeto e `http://localhost:3000`.
- Dockerfile multi-stage compila com Maven/Temurin 21 e executa com JRE 21 como usuário não root.
- O Compose espera a saúde do PostgreSQL antes de iniciar a API e expõe banco e API em endereços/portas configuráveis.
- Logs SQL detalhados ficam restritos ao profile `test`.

O volume `postgresql_data` é separado do volume usado pelo MySQL anterior. Nem o volume nem os dados MySQL são migrados automaticamente pelo Compose ou pelo Flyway; ambientes com dados a preservar precisam de migração explícita de dados antes da troca.

`flyway repair` não é um mecanismo de migração de banco ou dados e não deve ser executado cegamente para reconciliar o histórico MySQL com o schema PostgreSQL.

Não há profile `dev`: o Compose local usa `test`. Os testes automatizados usam
PostgreSQL real descartável via Testcontainers; não há banco em memória.

## Limites atuais que afetam o objetivo

- Contratos que retornam `User` podem expor o hash de senha.
- A listagem de pacientes é pública.
- Não há API para consultar/alterar clínica, criar FILIAL ou mudar status.
- Não existem comandos nem regras de transição entre os estados já aceitos por Java e pelo schema.
- A unicidade global legada do nome de paciente ainda atravessa clínicas.

Esses limites devem ser considerados antes de declarar o sistema pronto para operação com dados reais ou múltiplas clínicas.

## Critério para evolução do objetivo

Uma nova funcionalidade somente deve integrar a visão oficial quando houver evidência em controller/use case, regra de autorização, persistência/migration quando necessária e teste. Ideias presentes apenas em comentários, badges, enums sem fluxo ou código não chamado devem permanecer como **Hipótese**, plano ou débito técnico.

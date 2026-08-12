# Objetivo do sistema

## Identificação

| Campo | Valor |
|---|---|
| Sistema | `agendamento-smart-api` |
| Data de referência | 12 de agosto de 2026 |
| Evidências | Controllers, services, entidades, repositories, migrations, segurança, `README.md` e configuração operacional |

Este documento descreve a capacidade implementada. O estado-alvo confirmado está em [Requisitos do MVP](REQUISITOS_DO_MVP.md), e as decisões ainda abertas estão em [Entrevista de descoberta](ENTREVISTA_DE_DESCOBERTA.md). Uma regra de produto confirmada, mas ausente do código, continua sendo descrita aqui como não implementada.

## Propósito principal

O sistema fornece uma API HTTP para autenticar usuários vinculados a clínicas, cadastrar e consultar pacientes e criar/consultar agendamentos desses pacientes. Ele centraliza esses dados em MySQL e protege parte das operações com JWT e papéis `ADMIN`/`USER`.

O README existente descreve uso em áreas médicas e clínicas de Psicologia e Fisioterapia. O código atual é genérico: não implementa prontuário, especialidade clínica ou regra própria de uma dessas áreas.

## Problemas que o sistema resolve atualmente

- Autenticação de usuários por login e senha BCrypt.
- Emissão e validação de token JWT para requisições stateless.
- Associação obrigatória de usuários e pacientes a uma clínica.
- Cadastro de usuário em clínica existente.
- Cadastro de paciente e listagem paginada.
- Criação de agendamento associado a paciente.
- Armazenamento de uma lista de patologias em JSON por agendamento.
- Consulta de agendamento por UUID.
- Versionamento do schema com Flyway.
- Consulta da versão da aplicação para diagnóstico de build.

O código não resolve conflitos de horário, disponibilidade de profissionais, duração, recursos/salas, notificações, pagamento ou histórico clínico.

## Atores envolvidos

| Ator | Capacidades efetivamente observadas |
|---|---|
| Cliente não autenticado | Efetuar login e listar pacientes; demais acessos dependem das regras de CORS e segurança |
| Usuário `USER` autenticado | Consultar próprio usuário, listar todos os usuários, criar/consultar agendamentos e cadastrar outro usuário, inclusive enviando o papel desejado |
| Usuário `ADMIN` autenticado | Capacidades de `USER` e criação de pacientes |
| Clínica | Entidade organizacional à qual usuários e pacientes pertencem; não possui API própria |
| Paciente | Entidade cadastrada em uma clínica e referenciada por agendamentos; não é um principal autenticável |
| Operação/implantação | Configura banco, segredo JWT, migrations, porta e versão; pode executar via Maven ou Docker Compose |

Como requisito confirmado, a clínica representa o tenant e deverá ser a fronteira de segurança. No código atual ela é apenas uma associação organizacional: o isolamento entre clínicas não está implementado nas consultas e autorizações.

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

### 3. Cadastro de usuário

Entrada: UUID de clínica no path; login, senha e papel no corpo.

1. O sistema verifica se o login já existe.
2. A clínica é localizada pelo UUID informado.
3. A senha é codificada com BCrypt.
4. O mapper cria a entidade e o repository persiste.

Resultado atual: HTTP 200 sem corpo. Embora o controller declare `UserResponseDTO`, ele não o retorna.

A rota efetiva exige apenas que o chamador esteja autenticado; não há verificação de papel nem de pertencimento à clínica.

### 4. Cadastro de paciente

Entrada: nome e UUID da clínica.

1. A rota exige papel `ADMIN`.
2. A clínica é localizada.
3. O nome é transformado em maiúsculas.
4. O sistema procura outro paciente com o mesmo nome em toda a base.
5. Um código sequencial calculado por `MAX + 1` é atribuído e o paciente é persistido.

Resultado: UUID, nome e UUID da clínica.

### 5. Listagem de pacientes

Entrada: parâmetros `Pageable` reconhecidos pelo Spring Data.

1. Todos os pacientes são consultados sem filtro por clínica.
2. As entidades são convertidas em DTOs.
3. A resposta inclui conteúdo, página, tamanho, totais e indicador de última página.

Resultado: página de pacientes. A rota é pública na configuração atual.

### 6. Criação de agendamento

Entrada: UUID do paciente, patologias, data/hora, hora adicional, status opcional no tipo Java e variante opcional.

1. O paciente é localizado por UUID.
2. O mapper cria a entidade de agendamento.
3. O service associa o paciente.
4. As patologias são serializadas em JSON e o agendamento é persistido.

Resultado: DTO com IDs, dados de agenda, status e timestamps.

Não há validação de conflito, clínica, data futura ou transição de status. Se o status for omitido, o mapper atual pode anular o default `AGENDADO` e causar erro de persistência.

### 7. Consulta de agendamento

Entrada: UUID do agendamento.

1. O repository busca o registro por ID.
2. O mapper devolve o DTO de resposta com o ID do paciente.

Resultado: um agendamento. Não há listagem nem filtro por clínica, paciente, período ou status.

## Funcionalidades centrais e estágio

| Área | Implementado | Não implementado no código analisado |
|---|---|---|
| Autenticação | Login, JWT, cookie, Bearer, papéis | Logout, refresh/revogação, recuperação de senha, bloqueio de conta |
| Usuários | Cadastro, listagem, usuário atual | Atualização, exclusão, convite, política segura de atribuição de papel |
| Clínicas | Entidade, repository, service sem consumidor, seed inicial | Endpoints de cadastro, consulta, atualização ou exclusão |
| Pacientes | Cadastro e listagem paginada | Busca por ID, atualização, exclusão e isolamento por clínica |
| Agendamentos | Criação e consulta por ID | Listagem, alteração, cancelamento, finalização, conflito e disponibilidade |
| Operação | Flyway, build info, Dockerfile/Compose | Health check HTTP, métricas, tracing e documentação OpenAPI |

## Visão de produto

O produto implementado é uma API inicial de gestão de agendamentos, ainda concentrada no cadastro mínimo de usuários, pacientes e compromissos. O modelo de clínica permite organizar os dados por estabelecimento, mas essa organização ainda não constitui uma fronteira de segurança.

A evolução confirmada é um SaaS multi-tenant para fisioterapia, Pilates e atividades relacionadas, com usuários operando somente dados da própria clínica. Uma `Unit` padrão será criada desde o onboarding; pacientes e profissionais serão compartilhados pelo tenant e agendamentos serão vinculados à unidade. O código atual ainda não materializa essa arquitetura de segurança e domínio.

O enum de agendamento sugere um ciclo `AGENDADO`, `ATENDENDO`, `CANCELADO` e `FINALIZADO`; porém não existem endpoints nem regras de transição, e o banco ainda não aceita `ATENDENDO`. Portanto, esse ciclo não deve ser tratado como funcionalidade existente.

O ciclo-alvo confirmado usa `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO`, com ações manuais no primeiro MVP. Recorrência, remarcação vinculada, cancelamento auditável, capacidade do serviço e bloqueios de sobreposição também são requisitos confirmados ainda não implementados.

## Contexto operacional

- Aplicação Java 21/Spring Boot executada como JAR único.
- Porta HTTP fixa `8082` na configuração da aplicação.
- Banco MySQL; o Compose usa MySQL 8.0 com volume persistente.
- Schema gerenciado pelo Flyway na inicialização.
- Configuração de conexão por `AGENDA_URL`, `AGENDA_DB_USER` e `AGENDA_DB_PASSWORD`.
- Assinatura JWT por `JWT_SECRET`, com default configurado para desenvolvimento.
- Origens CORS globais: frontend Vercel do projeto e `http://localhost:3000`.
- Dockerfile multi-stage compila com Maven/Temurin 21 e executa com JRE 21 como usuário não root.
- O Compose espera a saúde do MySQL antes de iniciar a API e expõe banco e API em endereços/portas configuráveis.
- Logs SQL estão habilitados em nível detalhado.

Não há perfil Spring separado para desenvolvimento, teste e produção. Também não há testes automatizados ou ambiente de teste em memória/banco dedicado.

## Limites atuais que afetam o objetivo

- A API não assegura confidencialidade entre clínicas.
- Contratos que retornam `User` podem expor o hash de senha.
- Um usuário autenticado pode atribuir papel `ADMIN` no cadastro.
- A listagem de pacientes é pública.
- Não existe API de clínica, embora clínica seja obrigatória para usuários e pacientes.
- Não existe agenda consultável por período; somente criação e busca por UUID.
- O estado `ATENDENDO` existe em Java, mas não no schema.
- Erros de negócio não possuem representação HTTP oficial.

Esses limites devem ser considerados antes de declarar o sistema pronto para operação com dados reais ou múltiplas clínicas.

## Critério para evolução do objetivo

Uma nova funcionalidade somente deve integrar a visão oficial quando houver evidência em controller/use case, regra de autorização, persistência/migration quando necessária e teste. Ideias presentes apenas em comentários, badges, enums sem fluxo ou código não chamado devem permanecer como **Hipótese**, plano ou débito técnico.

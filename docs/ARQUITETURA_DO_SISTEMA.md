# Arquitetura do Sistema

## Identificação do documento

| Campo | Valor |
|---|---|
| Sistema | `agendamento-smart-api` |
| Artefato Maven | `com.agendamento-smart:agendamento-smart-api:1.0.5` |
| Escopo analisado | `pom.xml`, configuração operacional, `src/main/java/com/agendamento/smart` e `src/main/resources` |
| Data de referência | 8 de novembro de 2026 |
| Fonte de verdade | Código, imports, anotações, chamadas, migrations e configurações presentes no repositório |

Este documento descreve o estado implementado. O desenho-alvo confirmado está em [Requisitos do MVP](REQUISITOS_DO_MVP.md); lacunas de decisão estão em [Entrevista de descoberta](ENTREVISTA_DE_DESCOBERTA.md). Requisito planejado não é evidência de implementação.

## Resumo arquitetural

O sistema é um monólito de implantação única construído com Java 21 e Spring Boot 3.4.5. O código é organizado principalmente por camadas técnicas (`controller`, `service`, `repository`, `mapper`, `model`, `dtos` e `infra`), com subpacotes de domínio para usuário, paciente, clínica e agendamento. Não existem módulos Maven, limites de compilação ou mecanismos que impeçam dependências indevidas entre esses pacotes.

O fluxo predominante é:

```text
Cliente HTTP
  -> Spring Security / SecurityFilter
  -> Controller REST
  -> Service
  -> Mapper MapStruct
  -> Repository Spring Data JPA
  -> Hibernate/JDBC
  -> PostgreSQL
```

Há exceções relevantes ao fluxo: `AuthenticationController` acessa repositories
diretamente; `DebugController` usa `JdbcTemplate`; e `CodeGeneratorListener`,
pertencente ao modelo, depende de repositories. Essas exceções aumentam o
acoplamento entre apresentação, persistência e domínio.

## Visão dos componentes

| Componente/pacote | Responsabilidade comprovada | Dependências principais |
|---|---|---|
| `AgendaSmartApplication` | Inicializar a aplicação e o component scan | Spring Boot |
| `controller` | Expor endpoints REST e adaptar requisições/respostas | Services, DTOs, repositories em dois controllers, Spring MVC/Security |
| `service` | Orquestrar autenticação, usuários, pacientes, clínicas, agendamentos e versão | Repositories, mappers, models e DTOs |
| `repository` | Persistir e consultar entidades | Spring Data JPA, entidades |
| `mapper` | Converter DTOs e entidades em código gerado | MapStruct, DTOs, entidades |
| `model` | Representar entidades JPA, enums e mapeamento `jsonb` | JPA/Hibernate, Lombok; repositories via listener |
| `dtos` | Definir contratos HTTP e paginação | Jakarta Validation; alguns DTOs dependem de entidades/enums |
| `infra.security` | Configurar autorização stateless, autenticar JWT e emitir/validar tokens | Spring Security, Auth0 Java JWT, `UserRepository` |
| `util` | Disponibilizar versão filtrada do projeto | Spring Configuration Properties |
| `resources/db/migration` | Criar e evoluir o schema PostgreSQL | Flyway, PostgreSQL |

## Modelo de dados implementado

| Entidade | Tabela | Identificador | Relações e restrições relevantes |
|---|---|---|---|
| `Clinic` | `CLINIC` | `UUID` nativo | `code` único; status `ATIVO`/`INATIVO`/`BLOQUEADA`; possui exatamente um `ADMIN` por índice parcial |
| `ClinicUnit` | `CLINIC_UNIT` | `UUID` nativo | pertence a uma clínica; status explícito; tipo `MATRIZ`/`FILIAL`; uma única MATRIZ por clínica |
| `User` | `USERS` | `UUID` nativo | login único sem diferenciar maiúsculas/minúsculas; pertence obrigatoriamente a uma clínica; papel `ADMIN` ou `USER` |
| `Professional` | `PROFESSIONAL` | `UUID` nativo | pertence a uma clínica e exatamente uma unidade da mesma clínica; vínculo opcional e único com usuário |
| `Patient` | `PATIENT` | `UUID` nativo | `code` único; pertence a uma clínica e a uma ou várias unidades dela por `PATIENT_UNIT` |
| `Appointment` | `APPOINTMENT` | `UUID` nativo | pertence a clínica, unidade, paciente e serviço; profissional é opcional; `pathology` usa `jsonb`; instantes absolutos usam `timestamptz`; exclusões relacionadas são restritivas |

As relações formam `Clinic -> ClinicUnit`, com usuários limitados à clínica,
profissionais exclusivos de uma unidade, pacientes associados a uma ou várias
unidades da mesma clínica e appointments vinculados à unidade.

## Modelo de domínio alvo confirmado

O contrato-alvo mínimo é `Tenant/Clinic -> Unit`, com `User`, `Patient`, `Professional`, `Service`, `Appointment`, `AppointmentSeries` e `AppointmentEvent`. `Clinic` representará o tenant/conta; unidades físicas são filhas. Pacientes e profissionais pertencem ao tenant, enquanto agendamentos pertencem também a uma unidade.

Todo registro de negócio possuirá `tenantId`, `createdAt` e `updatedAt`, além de autoria quando relevante. O agendamento já guarda snapshots da duração e do preço, autoria e vínculos de tenant/unidade. Eventos de transição, remarcação e cancelamento imutáveis ainda não existem.

## Endpoints e proteção observada

| Método e rota | Comportamento | Proteção efetiva |
|---|---|---|
| `POST /auth/login` | Autentica login/senha, emite JWT no corpo e no cookie `jwt` | Pública |
| `POST /auth/register/clinic` | Cria clínica, MATRIZ, equipe e contas iniciais atomicamente | Pública |
| `GET /auth/list` | Lista todos os usuários | JWT + `ADMIN` ou `USER` |
| `GET /auth/me` | Retorna o usuário autenticado | JWT + `ADMIN` ou `USER` |
| `POST /user/register/{clinicId}` | Cria `USER` na clínica autenticada e, opcionalmente, seu profissional na MATRIZ | Qualquer usuário autenticado da mesma clínica |
| `GET /patient/list` | Lista paginada de todos os pacientes | Pública |
| `POST /patient/save` | Cria paciente na clínica autenticada e associa uma ou várias unidades | JWT + `ADMIN` |
| `POST /api/appointments` | Cria agendamento validando tenant, unidade, vínculos e status | Qualquer usuário autenticado |
| `GET /api/appointments` | Lista por intervalo e clínica, com `unitId` opcional | Qualquer usuário autenticado |
| `GET /api/appointments/{id}` | Consulta por UUID e clínica, respeitando status de leitura | Qualquer usuário autenticado |
| `GET /api/build-info/version` | Retorna a versão do build/projeto | Qualquer usuário autenticado |
| `GET /debug/db` | Retorna o banco selecionado pela conexão | Qualquer usuário autenticado |

`SecurityConfiguration` libera o onboarding em `POST /auth/register/clinic`.
O cadastro posterior permanece em `/user/register/{clinicId}` e exige
autenticação; o service confere a clínica e recusa papel `ADMIN`.

## Principais fluxos técnicos

### Autenticação e autorização

1. `AuthenticationController` cria um `UsernamePasswordAuthenticationToken`.
2. O `AuthenticationManager` usa `AuthorizationService`, que consulta `UserRepository.findByLogin`.
3. `TokenService` assina um JWT HMAC-SHA256 com emissor `agendamento-smart-api` e expiração de duas horas.
4. O controller devolve o token no corpo e em cookie HTTP-only com duração de uma hora.
5. Em requisições posteriores, `SecurityFilter` aceita Bearer token ou cookie; quando ambos existem, o cookie substitui o valor do header.
6. O filtro valida o token, recarrega o usuário pelo login e preenche o `SecurityContext`.

### Cadastro e consulta de pacientes

1. `PatientController` recebe `PatientRequestDTO` e o principal autenticado.
2. `PatientService` deriva a clínica da sessão e valida todos os `unitIds` nela.
3. O nome é convertido para maiúsculas e consultado globalmente para detectar duplicidade legada.
4. O service persiste paciente e associações `PATIENT_UNIT` na mesma transação.
5. `CodeGeneratorListener` consulta o maior código e atribui o próximo valor antes da persistência.
6. A listagem legada usa `Pageable`, ainda sem isolamento uniforme.

### Criação e consulta de agendamentos

1. `AppointmentController` recebe `AppointmentRequest` validado pelas constraints Jakarta Validation.
2. `AppointmentService` resolve, no tenant autenticado, paciente, unidade, serviço e profissional opcional.
3. O service exige clínica/unidade ativas, valida vínculos do paciente e do profissional opcional, conflitos/capacidade e persiste.
4. Hibernate mapeia a lista `pathology` diretamente para a coluna PostgreSQL `jsonb` com `@JdbcTypeCode(SqlTypes.JSON)`.
5. As consultas por UUID, clínica/intervalo ou unidade/intervalo respeitam tenant e regras de leitura por status.

### Evolução do banco

Com `spring.jpa.hibernate.ddl-auto=validate`, o Flyway `11.14.1` cria e evolui o schema, enquanto o Hibernate apenas valida sua compatibilidade com as entidades no startup. As migrations `V001` a `V008` criam o schema PostgreSQL, a `V009` renomeia a estrutura de agendamentos para `APPOINTMENT` e a `V010` adiciona status, tipos, vínculos e invariantes da estrutura clínica. A execução é estrita, com `baseline-on-migrate=false` e `out-of-order=false`.

O caminho comum, de homologação e de produção é somente `classpath:db/migration`. As seeds repetíveis de desenvolvimento ficam separadas em `classpath:db/seed/dev` e são adicionadas pelo profile `test`, ativado por padrão no Compose. `homo` e `prod` não recebem dados de demonstração.

## Padrões utilizados

| Padrão | Aplicação observada | Limitação atual |
|---|---|---|
| Arquitetura em camadas | Controllers, services, repositories, mappers e model separados por pacotes | Controllers acessam repositories; model listener acessa persistência |
| REST Controller | Endpoints com Spring MVC e `ResponseEntity` | Rotas e status HTTP não seguem uma convenção uniforme |
| Service Layer | Onboarding, usuário, paciente e agendamento usam services transacionais | Login ainda concentra orquestração no controller |
| Repository | Interfaces Spring Data JPA por entidade | Alguns métodos herdados são redeclarados e `UserRepository` retorna `UserDetails` diretamente |
| DTO | Records de entrada/saída e wrapper de paginação | Login e usuário expõem entidades JPA nos contratos |
| Mapper | MapStruct com `componentModel = "spring"` | `UserController` repete atribuições já feitas pelo mapper |
| Injeção de dependência | Beans gerenciados pelo Spring | Mistura construtor, Lombok e field injection; listener usa estado estático |
| JWT stateless | Filtro por requisição, sem sessão de servidor; segredo externo obrigatório | Cookie e token têm tempos diferentes; atributos do cookie não variam por ambiente |
| Database migration | Flyway versiona o schema PostgreSQL; seeds de desenvolvimento têm caminho separado | Os scripts atuais constroem uma base PostgreSQL nova, não convertem uma base MySQL existente |
| Externalized configuration | Conexão e segredo vêm do ambiente; profiles `test`, `homo` e `prod` controlam seeds e logs | O Compose fornece defaults locais e não deve ser promovido para `homo`/`prod` |

## Regras arquiteturais existentes

Estas regras são impostas pelo código ou pelo banco atualmente:

- O processo deve executar com Java 21 e empacota um único JAR Spring Boot.
- A conexão usa PostgreSQL e requer `AGENDA_URL`, `AGENDA_DB_USER` e `AGENDA_DB_PASSWORD` no runtime normal.
- Hibernate não cria nem altera tabelas; mudanças persistentes devem ser migrations Flyway.
- Usuários, pacientes e profissionais sempre possuem clínica; profissional possui uma unidade e paciente ao menos uma associação de unidade após o cadastro pela API.
- Índices parciais impedem mais de um `ADMIN` e mais de uma MATRIZ por clínica.
- Logins são únicos por restrição do banco.
- Códigos de clínica e paciente são únicos por restrição do banco.
- A autenticação é stateless e usa o login como `subject` do JWT.
- `ADMIN` recebe `ROLE_ADMIN` e `ROLE_USER`; `USER` recebe apenas `ROLE_USER`.
- Toda rota não liberada explicitamente exige autenticação.
- Apenas a criação de paciente exige `ADMIN` em nível de método; os demais fluxos aplicam suas regras no service.

O fluxo de `Appointment` deriva a clínica do usuário autenticado, restringe os
recursos ao tenant, valida status e vínculos por unidade, exige início futuro e
bloqueia conflitos de paciente/profissional e estouro de capacidade. O endpoint
legado público de listagem de pacientes ainda não possui isolamento uniforme, e
não há transições de status de appointment.

Como regra-alvo geral, tenant e autorização devem ser derivados da sessão; o cliente não escolhe a fronteira de segurança. No fluxo atual de criação, as três invariantes de agenda — paciente sem sobreposição, profissional sem sobreposição e capacidade do serviço não excedida — são verificadas sob bloqueio pessimista da clínica.

## Convenções técnicas observadas

- Pacote raiz: `com.agendamento.smart`.
- Entidades em singular e tabelas em maiúsculas.
- IDs de domínio usam `java.util.UUID` e o tipo PostgreSQL `UUID` nativo.
- DTOs preferencialmente como Java records.
- Instantes absolutos de `Appointment` usam `Instant`/`timestamptz`; os campos locais legados continuam como `LocalDateTime`/`LocalTime`, acompanhados do identificador de fuso da unidade.
- A lista `pathology` é persistida como `jsonb`.
- Mappers são interfaces MapStruct injetáveis como beans Spring.
- Repositories estendem `JpaRepository<Entidade, UUID>`.
- Nomes de classes e pacotes do módulo usam o vocabulário canônico `Appointment`/`appointment`; mensagens e comentários permanecem em português.
- Algumas classes usam constructor injection, outras field injection. Não existe uma convenção única efetivamente aplicada.
- Rotas misturam prefixos com e sem `/api` e verbos na URL (`/list`, `/save`, `/register`).

## Dependências críticas

| Dependência | Versão/origem | Uso e impacto |
|---|---|---|
| Java | 21 | Plataforma de compilação e execução |
| Spring Boot | 3.4.5 | Gerenciamento do build e infraestrutura web, JPA, Security e Validation |
| Spring Data JPA/Hibernate | Gerenciada pelo Spring Boot | Persistência de todas as entidades; forte dependência das anotações JPA |
| Spring Security | Gerenciada pelo Spring Boot | Autenticação, autorização por papel, CORS e filtro JWT |
| PostgreSQL JDBC Driver | Gerenciada pelo Spring Boot | Único driver configurado |
| PostgreSQL | 18.1 no ambiente anteriormente validado; o Compose atual usa `bitnami/postgresql:latest` | Porta `5432`; usa `UUID`, `jsonb`, checks e `timestamptz`; a tag atual é flutuante e ambientes persistentes devem fixar tag imutável/digest |
| Flyway | 11.14.1 | Fonte de verdade do schema; suporte PostgreSQL pelo módulo `flyway-database-postgresql` |
| MapStruct | 1.5.5.Final | Geração de mappers no compile; processor está como dependência `provided` |
| Lombok | Gerenciada pelo Spring Boot | Geração de builders, construtores e accessors |
| Auth0 Java JWT | 4.4.0 | Emissão e validação de tokens HMAC |
| Jackson | Gerenciada pelo Spring Boot | JSON HTTP e suporte ao mapeamento Hibernate da lista de patologias para `jsonb` |
| Docker/Compose | Imagens Temurin 21, Maven 3.9.9 e `bitnami/postgresql:latest` | Build em duas etapas, API com usuário não root e dados PostgreSQL no volume `postgresql_data`; PostgreSQL precisa de tag/digest fixo antes de ambiente persistente |

O sistema não possui adaptadores que permitam trocar PostgreSQL, Spring Security ou JPA sem alteração ampla no código e nas migrations.

O volume `postgresql_data` é independente do antigo volume MySQL. A troca do serviço no Compose não migra dados automaticamente; preservar dados legados exige exportação, transformação e importação explícitas.

Um `flyway repair` não converte schema ou dados e não deve ser usado para simplesmente aceitar checksums do histórico MySQL. Qualquer reparo exige validar previamente o banco alvo e o histórico esperado.

## Acoplamentos e violações arquiteturais

| Achado | Evidência | Consequência |
|---|---|---|
| Apresentação acoplada à persistência | `AuthenticationController` ainda injeta repositories | Parte do fluxo de login permanece fora de um service de aplicação |
| Modelo acoplado a repositories | `CodeGeneratorListener` importa `ClinicRepository` e `PatientRepository` | Callback JPA depende do Spring e de estado estático |
| Contrato HTTP acoplado a entidade | `LoginResponseDTO` contém `User`; `UserResponseDTO` contém `Clinic` | Mudanças JPA/serialização vazam para a API |
| Segurança acoplada ao repository | `SecurityFilter` e `AuthorizationService` consultam `UserRepository` diretamente | Autenticação depende do modelo persistente concreto |
| Agendamento acoplado a cadastros | `AppointmentService` acessa repositories de paciente, unidade, serviço e profissional | Não há fronteira autônoma para o módulo de agendamento |
| Compatibilidade legada no paciente | DTO ainda carrega `clinicId`, embora o service derive a clínica da sessão | Campo redundante precisa ser removido após migração dos clientes |
| Enum de DTO acoplado ao modelo | Responses importam `AppointmentStatus` | Contrato externo muda junto ao enum persistente |

## Módulos órfãos ou parcialmente integrados

- `ClinicService.createClinic` legado continua sem consumidor; o onboarding efetivo usa `ClinicOnboardingService` e `POST /auth/register/clinic`.
- `PatientServiceInt` é implementada por `PatientService`, mas consumidores injetam a classe concreta; a interface não cria um limite real.
- `AppointmentRepository.findById` apenas redeclara método já fornecido por `JpaRepository`.
- `UserMapper.INSTANCE` não é usado e duplica o modelo de acesso por bean Spring.
- `UserResponseDTO` permanece sem consumidor; o cadastro posterior declara `ResponseEntity<Void>`.
- Não existem fluxos HTTP para listar/alterar clínicas, atualizar/excluir pacientes, alterar/cancelar agendamentos ou encerrar sessão.

## Riscos técnicos priorizados

### Críticos

1. **Exposição de credenciais derivadas:** `LoginResponseDTO` e `/auth/list` serializam `User`, cujo `password` não tem `@JsonIgnore`. O hash BCrypt pode ser enviado ao cliente.
2. **Listagem pública de pacientes:** `/patient/list` permanece pública e sem filtro por clínica, fora do isolamento aplicado aos cadastros e à agenda.
3. **Migração dos dados legados externos:** Flyway evolui o schema PostgreSQL existente, mas não transporta dados nem volume de um MySQL anterior; essa transição exige exportação, conversão, importação e validação.

### Altos

1. `JWT_COOKIE_SECURE` controla o atributo `Secure` do cookie; `prod` usa `true` por padrão. Token e cookie expiram em duas e uma hora, respectivamente.
2. `CodeGeneratorListener` calcula `MAX(code) + 1`; inserções concorrentes podem gerar o mesmo código. O uso de repositories estáticos também dificulta ciclos de contexto.
3. A detecção de paciente duplicado é uma consulta seguida de insert, sem restrição correspondente no banco, e permanece global entre clínicas. Há condição de corrida.
4. O tratamento global cobre validação e erros de domínio dos novos fluxos, mas endpoints legados ainda podem produzir respostas não uniformes.
5. O bloqueio pessimista serializa criação de appointments por clínica e pode limitar paralelismo sob carga.

### Médios

- `@Valid` é aplicado a `AuthenticationDTO`, que ainda não possui constraints declarativas.
- O cadastro posterior retorna HTTP 200 sem corpo por contrato de compatibilidade.
- `@CrossOrigin(origins = "http:localhost:3000")` está sem `//` no controller de autenticação e conflita com a configuração global correta.
- O profile `test` habilita o logger SQL em `DEBUG`, mas mantém os valores dos parâmetros fora dos logs; ainda assim, ele deve ser usado somente em bases locais/descartáveis.
- `appointmentDate` já contém data e hora, mas `hours` armazena outra hora sem validação de consistência.
- Ainda não há transições de status, remarcação, cancelamento, recorrência nem horizonte máximo de agendamento.
- Injeção de dependência, caminhos REST, idioma de pacotes e estratégia de exceções são inconsistentes.

## Diretrizes para futuras implementações

### Regras obrigatórias recomendadas

1. Controllers devem depender de services/use cases, nunca de repositories.
2. Entidades não devem depender de repositories ou beans Spring. Geração de código deve usar mecanismo transacional e concorrente seguro no service/banco.
3. Contratos HTTP devem conter apenas DTOs; nunca retornar `User`, `Clinic` ou outra entidade JPA diretamente.
4. Toda operação sobre paciente, usuário ou agendamento deve derivar e validar a clínica a partir do principal autenticado. IDs fornecidos pelo cliente não bastam como autorização.
5. Atribuição de papel, especialmente `ADMIN`, deve ter política explícita e teste de autorização.
6. Alterações de entidade/enum persistente devem incluir migration compatível e teste de integração com PostgreSQL.
7. Novos endpoints devem seguir um prefixo e convenção únicos, preferencialmente recursos REST sob `/api`.
8. Validação de entrada deve ser declarativa e acompanhada de respostas de erro padronizadas por `@ControllerAdvice`.
9. Escritas compostas devem ocorrer em services com fronteira `@Transactional` clara.
10. Segredos devem vir do ambiente ou de secret manager; nunca do POM, código, imagem ou documentação.
11. Novos módulos de negócio devem usar o vocabulário canônico definido nos requisitos; compatibilidade com `Clinic`, `USER`, `ATENDENDO` e `FINALIZADO` deve ser tratada explicitamente em migration e contrato.
12. Nenhuma FK ou operação em cascata pode apagar agendamentos ou eventos de auditoria.

### Ordem de evolução sugerida

1. Remover entidades das respostas e impedir exposição de senha.
2. Corrigir cadastro de usuário, política de papéis e escopo por clínica.
3. Alinhar `AppointmentStatus` e o schema; tratar status default.
4. Criar exceções de domínio e tratamento HTTP global.
5. Introduzir testes de segurança, service, repository/migration e contratos dos endpoints.
6. Remover dependências de repository dos controllers e do listener JPA.
7. Padronizar rotas, injeção por construtor, nomenclatura e DTOs.
8. Automatizar a validação dos profiles e integrar segredos externos ao processo de deploy.

### Testes mínimos para cada novo fluxo

- Caso feliz e validações de entrada.
- Usuário sem autenticação, papel insuficiente e tentativa entre clínicas.
- Registro inexistente, duplicado e concorrência relevante.
- Compatibilidade entre enum Java, constraints PostgreSQL e mapeamento JPA/Hibernate.
- Serialização garantindo ausência de `password` e detalhes internos.
- Aplicação das migrations em banco PostgreSQL vazio com `homo`/`prod`, e com seeds no profile `test`; validar também a sobrescrita por `AGENDA_FLYWAY_LOCATIONS`.

## Manutenção desta documentação

Ao alterar controllers, DTOs, entidades, services, regras de segurança, dependências ou migrations:

1. Atualize o README do pacote afetado.
2. Atualize as tabelas de endpoints, dependências, riscos e módulos deste documento quando aplicável.
3. Atualize `OBJETIVO_DO_SISTEMA.md` se o fluxo ou o escopo de produto mudar.
4. Confirme a documentação por busca de imports/chamadas e execute `./mvnw clean verify`.
5. Marque interpretações não comprovadas como **Hipótese**.

Os READMEs de pacote são um inventário do estado atual; este documento é a referência para relações e regras transversais.

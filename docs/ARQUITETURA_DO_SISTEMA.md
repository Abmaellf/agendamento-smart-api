# Arquitetura do Sistema

## Identificação do documento

| Campo | Valor |
|---|---|
| Sistema | `agendamento-smart-api` |
| Artefato Maven | `com.agendamento-smart:agendamento-smart-api:1.0.5` |
| Escopo analisado | `pom.xml`, configuração operacional, `src/main/java/com/agendamento/smart` e `src/main/resources` |
| Data de referência | 12 de agosto de 2026 |
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
  -> MySQL 8
```

Há exceções relevantes ao fluxo: `AuthenticationController` e `UserController` acessam repositories diretamente; `DebugController` usa `JdbcTemplate`; e `CodeGeneratorListener`, pertencente ao modelo, depende de repositories. Essas exceções aumentam o acoplamento entre apresentação, persistência e domínio.

## Visão dos componentes

| Componente/pacote | Responsabilidade comprovada | Dependências principais |
|---|---|---|
| `AgendaSmartApplication` | Inicializar a aplicação e o component scan | Spring Boot |
| `controller` | Expor endpoints REST e adaptar requisições/respostas | Services, DTOs, repositories em dois controllers, Spring MVC/Security |
| `service` | Orquestrar autenticação, usuários, pacientes, clínicas, agendamentos e versão | Repositories, mappers, models e DTOs |
| `repository` | Persistir e consultar entidades | Spring Data JPA, entidades |
| `mapper` | Converter DTOs e entidades em código gerado | MapStruct, DTOs, entidades |
| `model` | Representar entidades JPA, enums e conversor JSON | JPA/Hibernate, Lombok, Jackson; repositories via listener |
| `dtos` | Definir contratos HTTP e paginação | Jakarta Validation; alguns DTOs dependem de entidades/enums |
| `infra.security` | Configurar autorização stateless, autenticar JWT e emitir/validar tokens | Spring Security, Auth0 Java JWT, `UserRepository` |
| `util` | Disponibilizar versão filtrada do projeto | Spring Configuration Properties |
| `resources/db/migration` | Criar e evoluir o schema MySQL | Flyway, MySQL |

## Modelo de dados implementado

| Entidade | Tabela | Identificador | Relações e restrições relevantes |
|---|---|---|---|
| `Clinic` | `CLINIC` | UUID em `BINARY(16)` | `code` único; possui usuários; migration cria a clínica inicial |
| `User` | `USERS` | UUID em `BINARY(16)` | login único no banco; pertence obrigatoriamente a uma clínica; papel `ADMIN` ou `USER` |
| `Patient` | `PATIENT` | UUID em `BINARY(16)` | `code` único; pertence obrigatoriamente a uma clínica |
| `Scheduling` | `SCHEDULING` | UUID em `BINARY(16)` | pertence obrigatoriamente a um paciente; patologias em JSON; exclusão do paciente apaga agendamentos |

As relações formam a cadeia `Clinic -> User` e `Clinic -> Patient -> Scheduling`. A aplicação não implementa, porém, filtros de consulta ou autorização que restrinjam dados à clínica do usuário autenticado.

## Modelo de domínio alvo confirmado

O contrato-alvo mínimo é `Tenant/Clinic -> Unit`, com `User`, `Patient`, `Professional`, `Service`, `Appointment`, `AppointmentSeries` e `AppointmentEvent`. `Clinic` representará o tenant/conta; unidades físicas são filhas. Pacientes e profissionais pertencem ao tenant, enquanto agendamentos pertencem também a uma unidade.

Todo registro de negócio possuirá `tenantId`, `createdAt` e `updatedAt`, além de autoria quando relevante. O agendamento guardará snapshots da duração e do preço. Eventos de transição, remarcação e cancelamento serão imutáveis e agendamentos não poderão ser apagados. O schema atual não atende ainda a essas invariantes.

## Endpoints e proteção observada

| Método e rota | Comportamento | Proteção efetiva |
|---|---|---|
| `POST /auth/login` | Autentica login/senha, emite JWT no corpo e no cookie `jwt` | Pública |
| `GET /auth/list` | Lista todos os usuários | JWT + `ADMIN` ou `USER` |
| `GET /auth/me` | Retorna o usuário autenticado | JWT + `ADMIN` ou `USER` |
| `POST /user/register/{clinicId}` | Cria usuário na clínica informada, com papel recebido no corpo | Qualquer usuário autenticado, por `anyRequest().authenticated()` |
| `GET /patient/list` | Lista paginada de todos os pacientes | Pública |
| `POST /patient/save` | Cria paciente na clínica informada | JWT + `ADMIN` |
| `POST /api/scheduling` | Cria agendamento para o paciente informado | Qualquer usuário autenticado |
| `GET /api/scheduling/{id}` | Consulta um agendamento por UUID | Qualquer usuário autenticado |
| `GET /api/build-info/version` | Retorna a versão do build/projeto | Qualquer usuário autenticado |
| `GET /debug/db` | Retorna o banco selecionado pela conexão | Qualquer usuário autenticado |

`SecurityConfiguration` libera `POST /auth/register/**`, mas não existe controller nessa rota. O cadastro real está em `/user/register/{clinicId}` e, portanto, cai na regra autenticada padrão. A regra específica para `GET /api/scheduling` também não corresponde ao endpoint implementado, que exige `/{id}`; a autenticação ocorre pela regra padrão.

## Principais fluxos técnicos

### Autenticação e autorização

1. `AuthenticationController` cria um `UsernamePasswordAuthenticationToken`.
2. O `AuthenticationManager` usa `AuthorizationService`, que consulta `UserRepository.findByLogin`.
3. `TokenService` assina um JWT HMAC-SHA256 com emissor `agendamento-smart-api` e expiração de duas horas.
4. O controller devolve o token no corpo e em cookie HTTP-only com duração de uma hora.
5. Em requisições posteriores, `SecurityFilter` aceita Bearer token ou cookie; quando ambos existem, o cookie substitui o valor do header.
6. O filtro valida o token, recarrega o usuário pelo login e preenche o `SecurityContext`.

### Cadastro e consulta de pacientes

1. `PatientController` recebe `PatientRequestDTO`.
2. `PatientService` resolve a clínica por UUID.
3. O nome é convertido para maiúsculas e consultado globalmente para detectar duplicidade.
4. `PatientMapper` cria a entidade, o service associa a clínica e o repository persiste.
5. `CodeGeneratorListener` consulta o maior código e atribui o próximo valor antes da persistência.
6. A listagem usa `Pageable`, mapeia cada entidade e devolve `PageResponseDTO`.

### Criação e consulta de agendamentos

1. `SchedulingController` recebe `SchedulingRequest` validado apenas nos campos anotados com `@NotNull`.
2. `SchedulingService` resolve o paciente por UUID.
3. `SchedulingMapper` converte o request; o service associa o paciente e persiste.
4. `StringListJsonConverter` serializa `pathology` para a coluna JSON.
5. A consulta busca somente por UUID e converte a entidade em `SchedulingResponse`.

### Evolução do banco

Com `spring.jpa.hibernate.ddl-auto=none`, o schema é responsabilidade do Flyway. Na inicialização, as migrations `V001` a `V005` criam clínica, usuários, pacientes e agendamentos. `spring.flyway.baseline-on-migrate=true` e `out-of-order=true` estão habilitados.

## Padrões utilizados

| Padrão | Aplicação observada | Limitação atual |
|---|---|---|
| Arquitetura em camadas | Controllers, services, repositories, mappers e model separados por pacotes | Controllers acessam repositories; model listener acessa persistência |
| REST Controller | Endpoints com Spring MVC e `ResponseEntity` | Rotas e status HTTP não seguem uma convenção uniforme |
| Service Layer | Regras de paciente e agendamento ficam em services | Cadastro de usuário e login ainda concentram orquestração no controller |
| Repository | Interfaces Spring Data JPA por entidade | Métodos herdados são redeclarados e há query nativa não utilizada |
| DTO | Records de entrada/saída e wrapper de paginação | Login e usuário expõem entidades JPA nos contratos |
| Mapper | MapStruct com `componentModel = "spring"` | `UserController` repete atribuições já feitas pelo mapper |
| Injeção de dependência | Beans gerenciados pelo Spring | Mistura construtor, Lombok e field injection; listener usa estado estático |
| JWT stateless | Filtro por requisição, sem sessão de servidor | Cookie e token têm tempos diferentes; configuração de produção não está separada |
| Database migration | Flyway versiona o schema | Enum do banco diverge do enum Java; plugin e runtime usam versões diferentes |
| Externalized configuration | Conexão e segredo podem vir do ambiente | Há defaults inseguros e credenciais literais no plugin Maven |

## Regras arquiteturais existentes

Estas regras são impostas pelo código ou pelo banco atualmente:

- O processo deve executar com Java 21 e empacota um único JAR Spring Boot.
- A conexão usa MySQL e requer `AGENDA_URL`, `AGENDA_DB_USER` e `AGENDA_DB_PASSWORD` no runtime normal.
- Hibernate não cria nem altera tabelas; mudanças persistentes devem ser migrations Flyway.
- Usuários e pacientes sempre possuem clínica; agendamentos sempre possuem paciente.
- Logins são únicos por restrição do banco.
- Códigos de clínica e paciente são únicos por restrição do banco.
- A autenticação é stateless e usa o login como `subject` do JWT.
- `ADMIN` recebe `ROLE_ADMIN` e `ROLE_USER`; `USER` recebe apenas `ROLE_USER`.
- Toda rota não liberada explicitamente exige autenticação.
- Apenas a criação de paciente exige `ADMIN` em nível de método.

Não há regra implementada que limite o acesso à clínica do usuário, valide transições de status, impeça choque de horários ou determine que um agendamento seja futuro.

Como regra-alvo, tenant e autorização devem ser derivados da sessão; o cliente não escolhe a fronteira de segurança. As três invariantes de agenda — paciente sem sobreposição, profissional sem sobreposição e capacidade do serviço não excedida — não admitem exceção administrativa e precisam ser protegidas também contra concorrência.

## Convenções técnicas observadas

- Pacote raiz: `com.agendamento.smart`.
- Entidades em singular e tabelas em maiúsculas.
- IDs de domínio em `UUID`, persistidos como `BINARY(16)`.
- DTOs preferencialmente como Java records.
- Datas e horas usam `LocalDateTime` e `LocalTime`, sem timezone no contrato.
- Mappers são interfaces MapStruct injetáveis como beans Spring.
- Repositories estendem `JpaRepository<Entidade, UUID>`.
- Nomes de classes e código estão em inglês; mensagens, comentários e alguns nomes de pacote misturam português e inglês (`agendamento`/`scheduling`).
- Algumas classes usam constructor injection, outras field injection. Não existe uma convenção única efetivamente aplicada.
- Rotas misturam prefixos com e sem `/api` e verbos na URL (`/list`, `/save`, `/register`).

## Dependências críticas

| Dependência | Versão/origem | Uso e impacto |
|---|---|---|
| Java | 21 | Plataforma de compilação e execução |
| Spring Boot | 3.4.5 | Gerenciamento do build e infraestrutura web, JPA, Security e Validation |
| Spring Data JPA/Hibernate | Gerenciada pelo Spring Boot | Persistência de todas as entidades; forte dependência das anotações JPA |
| Spring Security | Gerenciada pelo Spring Boot | Autenticação, autorização por papel, CORS e filtro JWT |
| MySQL Connector/J | Gerenciada pelo Spring Boot | Único driver configurado |
| MySQL | 8.0 no Compose | Usa `BINARY(16)`, `JSON`, `ENUM` e funções específicas em query nativa |
| Flyway | 10.20.0 no runtime; plugin Maven 10.0.0 | Fonte de verdade do schema; versões desalinhadas |
| MapStruct | 1.5.5.Final | Geração de mappers no compile; processor está como dependência `provided` |
| Lombok | Gerenciada pelo Spring Boot | Geração de builders, construtores e accessors |
| Auth0 Java JWT | 4.4.0 | Emissão e validação de tokens HMAC |
| Jackson | Gerenciada pelo Spring Boot | JSON HTTP e conversão manual da lista de patologias |
| Docker/Compose | Imagens Temurin 21, Maven 3.9.9 e MySQL 8.0 | Build em duas etapas e execução com usuário não root |

O sistema não possui adaptadores que permitam trocar MySQL, Spring Security ou JPA sem alteração ampla no código e nas migrations.

## Acoplamentos e violações arquiteturais

| Achado | Evidência | Consequência |
|---|---|---|
| Apresentação acoplada à persistência | `AuthenticationController` e `UserController` injetam repositories | Regras ficam espalhadas e testes de controller exigem mais dependências |
| Modelo acoplado a repositories | `CodeGeneratorListener` importa `ClinicRepository` e `PatientRepository` | Callback JPA depende do Spring e de estado estático |
| Contrato HTTP acoplado a entidade | `LoginResponseDTO` contém `User`; `UserResponseDTO` contém `Clinic` | Mudanças JPA/serialização vazam para a API |
| Segurança acoplada ao repository | `SecurityFilter` e `AuthorizationService` consultam `UserRepository` diretamente | Autenticação depende do modelo persistente concreto |
| Agendamento acoplado a paciente | `SchedulingService` acessa `PatientRepository`; entidade tem `ManyToOne` | Não há fronteira autônoma para o módulo de agendamento |
| Paciente acoplado a clínica | DTO carrega `clinicId`; service acessa `ClinicRepository` | O chamador escolhe a clínica sem política central de escopo |
| Enum de DTO acoplado ao modelo | Requests/responses importam `StatusScheduling` | Contrato externo muda junto ao enum persistente |

## Módulos órfãos ou parcialmente integrados

- `ClinicService.createClinic` não possui controller nem chamada encontrada no código. A única clínica inicial é criada pela migration `V001`.
- `PatientServiceInt` é implementada por `PatientService`, mas consumidores injetam a classe concreta; a interface não cria um limite real.
- `ClinicRepository.findByUuid` não possui chamada ativa.
- `SchedulingRepository.findById` apenas redeclara método já fornecido por `JpaRepository`.
- `UserMapper.INSTANCE` não é usado e duplica o modelo de acesso por bean Spring.
- `UserResponseDTO` aparece como tipo declarado do cadastro, mas o endpoint devolve corpo vazio.
- Não existem fluxos HTTP para criar/listar clínicas, atualizar/excluir pacientes, listar/alterar/cancelar agendamentos ou encerrar sessão.

## Riscos técnicos priorizados

### Críticos

1. **Exposição de credenciais derivadas:** `LoginResponseDTO` e `/auth/list` serializam `User`, cujo `password` não tem `@JsonIgnore`. O hash BCrypt pode ser enviado ao cliente.
2. **Ausência de isolamento por clínica:** listagens e buscas não usam a clínica do usuário. Um usuário autenticado pode consultar agendamentos por UUID e cadastrar agendamentos/pacientes para IDs de outras clínicas; `/patient/list` é público.
3. **Elevação de privilégio no cadastro:** qualquer usuário autenticado pode chamar `/user/register/{clinicId}`, escolher qualquer clínica existente e enviar `role=ADMIN`.
4. **Segredo no build:** o plugin Flyway no `pom.xml` contém usuário e senha literais. Além da exposição, esses valores divergem da configuração por ambiente usada pela aplicação.
5. **Schema incompatível com o código:** `StatusScheduling` contém `ATENDENDO`, mas o `ENUM` criado em `V004` aceita somente `AGENDADO`, `CANCELADO` e `FINALIZADO`. Persistir `ATENDENDO` falha no MySQL.

### Altos

1. A regra pública de cadastro aponta para `/auth/register/**`, rota inexistente; o endpoint real tem outra proteção.
2. O JWT possui segredo default, o cookie sempre usa `secure=false`, e token/cookie expiram em duas e uma hora, respectivamente.
3. O mapper de agendamento atribui `null` ao status quando o request omite o campo, anulando o default da entidade e violando a coluna `NOT NULL`.
4. `CodeGeneratorListener` calcula `MAX(code) + 1`; inserções concorrentes podem gerar o mesmo código. O uso de repositories estáticos também dificulta testes e ciclos de contexto.
5. A detecção de paciente duplicado é uma consulta seguida de insert, sem restrição correspondente no banco, e é global entre clínicas. Há condição de corrida.
6. `PatientService` usa `com.sun.jdi.request.DuplicateRequestException`, exceção da API de depuração do JDK, como erro de negócio; isso acopla o runtime a um módulo inadequado.
7. Não há tratamento global de erros; diversas ausências e conflitos viram `RuntimeException`/`IllegalArgumentException`, sem contrato HTTP estável.
8. Não existem testes automatizados, apesar da presença de `spring-boot-starter-test`.

### Médios

- `@Valid` é aplicado a DTOs que não possuem constraints (`AuthenticationDTO`, `RegisterDTO`, `PatientRequestDTO`).
- O endpoint de cadastro declara `UserResponseDTO`, mas retorna HTTP 200 sem corpo.
- `@CrossOrigin(origins = "http:localhost:3000")` está sem `//` no controller de autenticação e conflita com a configuração global correta.
- `spring.jpa.show-sql=true` e loggers SQL em `DEBUG`/`TRACE` podem expor dados e gerar volume excessivo fora do desenvolvimento.
- `dateScheduling` já contém data e hora, mas `hours` armazena outra hora sem validação de consistência.
- Não há regras de choque de agenda, janela temporal, transição de status ou vínculo do agendamento à clínica autenticada.
- `Flyway` runtime e plugin Maven usam versões diferentes; `out-of-order=true` reduz a previsibilidade da sequência em ambientes compartilhados.
- Injeção de dependência, caminhos REST, idioma de pacotes e estratégia de exceções são inconsistentes.

## Diretrizes para futuras implementações

### Regras obrigatórias recomendadas

1. Controllers devem depender de services/use cases, nunca de repositories.
2. Entidades não devem depender de repositories ou beans Spring. Geração de código deve usar mecanismo transacional e concorrente seguro no service/banco.
3. Contratos HTTP devem conter apenas DTOs; nunca retornar `User`, `Clinic` ou outra entidade JPA diretamente.
4. Toda operação sobre paciente, usuário ou agendamento deve derivar e validar a clínica a partir do principal autenticado. IDs fornecidos pelo cliente não bastam como autorização.
5. Atribuição de papel, especialmente `ADMIN`, deve ter política explícita e teste de autorização.
6. Alterações de entidade/enum persistente devem incluir migration compatível e teste de integração com MySQL.
7. Novos endpoints devem seguir um prefixo e convenção únicos, preferencialmente recursos REST sob `/api`.
8. Validação de entrada deve ser declarativa e acompanhada de respostas de erro padronizadas por `@ControllerAdvice`.
9. Escritas compostas devem ocorrer em services com fronteira `@Transactional` clara.
10. Segredos devem vir do ambiente ou de secret manager; nunca do POM, código, imagem ou documentação.
11. Novos módulos de negócio devem usar o vocabulário canônico definido nos requisitos; compatibilidade com `Clinic`, `Scheduling`, `USER`, `ATENDENDO` e `FINALIZADO` deve ser tratada explicitamente em migration e contrato.
12. Nenhuma FK ou operação em cascata pode apagar agendamentos ou eventos de auditoria.

### Ordem de evolução sugerida

1. Remover entidades das respostas e impedir exposição de senha.
2. Corrigir cadastro de usuário, política de papéis e escopo por clínica.
3. Alinhar `StatusScheduling` e o schema; tratar status default.
4. Criar exceções de domínio e tratamento HTTP global.
5. Introduzir testes de segurança, service, repository/migration e contratos dos endpoints.
6. Remover dependências de repository dos controllers e do listener JPA.
7. Padronizar rotas, injeção por construtor, nomenclatura e DTOs.
8. Separar configuração local/produção e reduzir logs sensíveis.

### Testes mínimos para cada novo fluxo

- Caso feliz e validações de entrada.
- Usuário sem autenticação, papel insuficiente e tentativa entre clínicas.
- Registro inexistente, duplicado e concorrência relevante.
- Compatibilidade entre enum Java, coluna MySQL e conversão MapStruct.
- Serialização garantindo ausência de `password` e detalhes internos.
- Aplicação das migrations em banco MySQL vazio.

## Manutenção desta documentação

Ao alterar controllers, DTOs, entidades, services, regras de segurança, dependências ou migrations:

1. Atualize o README do pacote afetado.
2. Atualize as tabelas de endpoints, dependências, riscos e módulos deste documento quando aplicável.
3. Atualize `OBJETIVO_DO_SISTEMA.md` se o fluxo ou o escopo de produto mudar.
4. Confirme a documentação por busca de imports/chamadas e execute `./mvnw test`.
5. Marque interpretações não comprovadas como **Hipótese**.

Os READMEs de pacote são um inventário do estado atual; este documento é a referência para relações e regras transversais.

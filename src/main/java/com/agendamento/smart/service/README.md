# Services transversais e de domínio

## Objetivo do módulo

Orquestrar autenticação Spring, clínicas, pacientes, usuário atual e informação de versão.

## Responsabilidades principais

- Carregar `UserDetails` por login.
- Cadastrar/listar pacientes com mapeamento DTO.
- Verificar login e recuperar o usuário atual.
- Criar clínica por repository.
- Fornecer versão do projeto.

## Funcionalidades existentes

- `AuthorizationService`: integração `UserDetailsService`.
- `PatientService`: paginação e cadastro, com escopo de clínica/unidade derivado da sessão.
- `UnitScopeService`: resolve todas as unidades da clínica para `ADMIN` e apenas `clinicUnit` para `USER`.
- `CatalogService`: fornece catálogos de pacientes, unidades, serviços e profissionais, filtrando pacientes por `unitId` autorizado quando solicitado.
- `UserService`: existência de login e principal atual.
- `ClinicService`: criação sem consumidor encontrado.
- `ProjectInfoService`: versão configurada.
- `PatientServiceInt`: interface parcial apenas para listagem.

`ServicePackageService` é transacional e permite a administradores criar, consultar, editar e ativar/desativar definições de pacote. A clínica é derivada do usuário autenticado; unidade e serviço são sempre resolvidos dentro dela. A listagem operacional também atende `USER`, forçando a unidade associada à sessão e apenas pacotes ativos. O serviço normaliza descrição e preço decimal, valida dias sem duplicidade e devolve a frequência semanal derivada.

`PackageAppointmentService` exige `unitId` no preview e na confirmação. O valor precisa pertencer ao escopo da sessão, o pacote precisa estar ativo nessa unidade e o paciente precisa estar vinculado à mesma unidade; a unidade também compõe o fingerprint da prévia. A contratação persiste contrato e agendamentos somente após essas validações.

`ServicePackageUsageService` é a extensão preparada para a SPEC 02. Nesta etapa ele informa que não há vínculos de agendamento e, portanto, a definição permanece editável. Quando a SPEC 02 criar o vínculo pacote-agendamento, a implementação deverá consultar esse vínculo para fazer `PATCH` e `PATCH /status` retornarem `PACKAGE_IMMUTABLE` sem persistir alteração parcial.

## Dependências internas e externas

- Internas: repositories, mappers, DTOs, entidades e `ProjectInfoProperties`.
- Externas: Spring Service/Security/Data, Jakarta Transactions, Lombok e uma exceção da API JDI do JDK.

## Módulos relacionados

Controllers, `repository`, `mapper`, `model`, `dtos`, `infra/security` e `service/appointment`.

## Pontos de entrada

- Controllers de autenticação, paciente e build info.
- `AuthenticationManager` chama `AuthorizationService`.
- `ClinicService` não possui ponto de entrada encontrado.

## Fluxos de entrada

- Autenticação: login -> repository -> `UserDetails`.
- Paciente: principal autenticado -> escopo de unidade -> consulta paginada -> mapper -> wrapper de página.
- Usuário atual: `SecurityContext` -> repository por UUID.
- Versão: controller -> properties.

## Arquivos críticos

- `AuthorizationService.java`.
- `PatientService.java`.
- `UserService.java`.
- `ClinicService.java`.
- `ProjectInfoService.java`.
- `PatientServiceInt.java`.

## Regras confirmadas para evolução do módulo

- Services/use cases são a fronteira transacional e de autorização do negócio; derivam usuário, papel e tenant da sessão.
- `ADMIN` e `BASIC` cadastram paciente/profissional e usuário básico; somente `ADMIN` cria/promove administrador, cancela agenda, redefine senha e altera configurações restritas.
- Onboarding cria tenant, unidade padrão e primeiro administrador de maneira atômica, mantendo a conta bloqueada até validar e-mail.
- Escritas validam que todos os IDs relacionados pertencem ao mesmo tenant e registram timestamps/autoria.
- Exceções de domínio devem ser próprias e mapeadas para um contrato HTTP estável.
- Regras completas de agenda ficam no service/use case de agendamento; controllers e mappers não as substituem.

## Observações técnicas e débitos identificados

- `PatientService` não filtra por clínica do usuário e verifica duplicidade de nome em toda a base.
- `PatientService` lança `com.sun.jdi.request.DuplicateRequestException`, tipo de infraestrutura de depuração, não erro de domínio.
- Ausência de clínica e usuário é representada por exceções genéricas, sem contrato HTTP global.
- `UserService.currentUserService` ignora o parâmetro `Authentication` recebido e consulta novamente o `SecurityContextHolder`.
- `PatientServiceInt` não é injetada pelos consumidores e cobre apenas um dos métodos do service.
- `ClinicService` é órfão no grafo de chamadas e aceita UUID externo mesmo com ID gerado pela entidade.
- A injeção mistura campos, construtores Lombok e construtores explícitos entre o pacote.
- Não existem testes dos services.

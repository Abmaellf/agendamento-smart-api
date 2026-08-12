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
- `PatientService`: paginação e cadastro.
- `UserService`: existência de login e principal atual.
- `ClinicService`: criação sem consumidor encontrado.
- `ProjectInfoService`: versão configurada.
- `PatientServiceInt`: interface parcial apenas para listagem.

## Dependências internas e externas

- Internas: repositories, mappers, DTOs, entidades e `ProjectInfoProperties`.
- Externas: Spring Service/Security/Data, Jakarta Transactions, Lombok e uma exceção da API JDI do JDK.

## Módulos relacionados

Controllers, `repository`, `mapper`, `model`, `dtos`, `infra/security` e `service/scheduling`.

## Pontos de entrada

- Controllers de autenticação, paciente e build info.
- `AuthenticationManager` chama `AuthorizationService`.
- `ClinicService` não possui ponto de entrada encontrado.

## Fluxos de entrada

- Autenticação: login -> repository -> `UserDetails`.
- Paciente: controller -> clínica/repository -> normalização/duplicidade -> mapper -> repository.
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

# DTOs transversais

## Objetivo do módulo

Definir contratos HTTP compartilhados e a estrutura de paginação da API.

## Responsabilidades principais

- Representar credenciais e resposta de login.
- Padronizar conteúdo e metadados de uma página Spring Data.
- Agrupar subpacotes de contratos de paciente, usuário e agendamento.

## Funcionalidades existentes

- `AuthenticationDTO`: login e senha.
- `LoginResponseDTO`: token e entidade de usuário.
- `PageResponseDTO<T>`: conteúdo, página, tamanho, totalizadores e última página.

## Dependências internas e externas

- Internas: `LoginResponseDTO` depende de `model/user/User`.
- Externas: `Page` do Spring Data.

## Módulos relacionados

Todos os controllers, services e mappers que compõem os contratos da API.

## Pontos de entrada

Desserialização de `AuthenticationDTO` no login; serialização de `LoginResponseDTO` e `PageResponseDTO` nas respostas.

## Fluxos de entrada

JSON HTTP -> records -> controller/service; `Page<T>` -> `PageResponseDTO.from` -> JSON HTTP.

## Arquivos críticos

- `AuthenticationDTO.java`.
- `LoginResponseDTO.java`.
- `PageResponseDTO.java`.

## Regras confirmadas para evolução do módulo

- Contratos canônicos cobrem `Tenant`, `Unit`, `User`, `Patient`, `Professional`, `Service`, `Appointment`, `AppointmentSeries` e `AppointmentEvent`.
- DTOs nunca expõem entidades JPA, senha/hash ou campos internos de segurança.
- `tenantId` retornado serve como contexto; requests comuns não o usam como autoridade. Agendamentos também carregam `unitId`.
- Registros mutáveis expõem timestamps e autoria quando aplicável; transições de agenda expõem seu histórico auditável.
- A API deve possuir um contrato de erro estável para validação, conflito, capacidade, estado inválido, ausência e proibição.

## Observações técnicas e débitos identificados

- `LoginResponseDTO` inclui `User`, vazando o modelo JPA e, no estado atual, o hash de senha.
- `AuthenticationDTO` não declara `@NotBlank` ou outras constraints.
- Não existe DTO padronizado de erro.

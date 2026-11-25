# Spec Delta

## Purpose

Oferece uma listagem de pacotes eficiente e navegável para consumidores da API,
aplicando filtros no servidor e retornando metadados suficientes para tabelas
paginadas sem ampliar o escopo de autorização ou domínio dos pacotes.

## ADDED Requirements

### Requirement: A listagem de pacotes aceita paginação server-side
`GET /api/service-packages` MUST aceitar `page` zero-based e `size`, aplicar a paginação no servidor e retornar `content`, `page`, `size`, `totalElements`, `totalPages` e `last`.

#### Scenario: Primeira página
- **WHEN** um cliente autenticado solicitar a listagem sem informar página
- **THEN** a API MUST retornar a primeira página usando os valores padrão e informar metadados coerentes com o total filtrado

#### Scenario: Página intermediária
- **WHEN** um cliente solicitar uma página existente com tamanho válido
- **THEN** a API MUST retornar somente os registros daquela página, o índice solicitado e `last` falso quando houver páginas posteriores

#### Scenario: Última página
- **WHEN** um cliente solicitar a última página existente
- **THEN** a API MUST retornar os registros restantes e `last` verdadeiro

### Requirement: A paginação preserva filtros e ordenação estável
O endpoint MUST aplicar conjuntamente `unitId`, `serviceId`, `active`, `totalSessions` e `name` antes de calcular os metadados, mantendo ordenação ascendente por nome.

#### Scenario: Página filtrada por nome e situação
- **WHEN** um cliente solicitar `name`, `active`, `page` e `size`
- **THEN** a API MUST calcular o total somente sobre os registros que atendem aos filtros e retornar a página nessa ordem

#### Scenario: Filtros sem resultados
- **WHEN** os filtros não encontrarem pacotes
- **THEN** a API MUST retornar `content` vazio, `totalElements` igual a zero, `totalPages` igual a zero e `last` verdadeiro

### Requirement: A consulta paginada mantém o escopo e as regras de acesso
O endpoint MUST derivar a clínica do principal autenticado; administradores só podem consultar sua clínica e usuários básicos só podem consultar pacotes ativos da única unidade autorizada.

#### Scenario: Administrador consulta a própria clínica
- **WHEN** um administrador solicitar uma página com ou sem filtro de unidade
- **THEN** a API MUST retornar somente pacotes da clínica autenticada e validar a unidade filtrada dentro dessa clínica

#### Scenario: Usuário básico consulta sua unidade
- **WHEN** um usuário básico solicitar uma página sem unidade ou com sua unidade autorizada
- **THEN** a API MUST forçar a unidade autorizada e `active=true` antes de paginar

#### Scenario: Usuário básico solicita unidade externa
- **WHEN** um usuário básico informar uma unidade diferente da única unidade autorizada
- **THEN** a API MUST responder `TENANT_RESOURCE_NOT_FOUND` com HTTP 404 sem consultar pacotes fora do escopo

### Requirement: Parâmetros de página inválidos são rejeitados
`page` MUST ser maior ou igual a zero e `size` MUST estar entre 1 e 100; valores ausentes MUST usar página zero e tamanho padrão 10.

#### Scenario: Página negativa
- **WHEN** um cliente enviar `page` menor que zero
- **THEN** a API MUST responder HTTP 400 com erro de validação sem consultar o repositório de pacotes

#### Scenario: Tamanho acima do limite
- **WHEN** um cliente enviar `size` maior que 100
- **THEN** a API MUST responder HTTP 400 com erro de validação sem aceitar um tamanho ilimitado


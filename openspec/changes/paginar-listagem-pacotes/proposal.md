# Proposal

## Why

`GET /api/service-packages` atualmente carrega todos os pacotes da clínica e
aplica filtros em memória, enquanto o frontend passou a consultar a listagem
como uma tabela paginada. Sem paginação no servidor, a tela não consegue
limitar o volume transferido nem informar navegação real entre páginas.

## What Changes

- Alterar a consulta de pacotes para aceitar `page` zero-based e `size`, além
  dos filtros atuais `unitId`, `serviceId`, `active`, `totalSessions` e `name`.
- Aplicar filtros e ordenação por nome no banco, retornando somente a página
  solicitada.
- Substituir o envelope `CatalogResponse` da listagem de pacotes por uma
  resposta paginada com `content`, `page`, `size`, `totalElements`,
  `totalPages` e `last`.
- Preservar o escopo por clínica, a regra de unidade para usuários básicos e
  os contratos de detalhe/mutação, sem adicionar exclusão ou novos campos de
  pacote.
- Definir limites e valores padrão seguros para `page` e `size`, rejeitando
  entradas inválidas conforme o padrão de erro da API.

Fora de escopo: alterações no frontend, novos filtros, mudanças de autorização,
migrations, alteração dos endpoints de criação/edição/status e paginação de
outros catálogos.

## Capabilities

### New Capabilities

- `service-package-listing`: fornece listagem administrativa/operacional de
  pacotes com filtros, ordenação estável, paginação e metadados de navegação.

### Modified Capabilities

- Nenhuma capability promovida em `openspec/specs/` cobre atualmente a
  listagem de pacotes. A change concluída `gerenciar-pacotes-flexiveis` é a
  referência de domínio e não será reescrita.

## Impact

- API: `ServicePackageController`, `ServicePackageService`,
  `ServicePackageRepository`, DTO de resposta paginada e README do endpoint.
- Contrato HTTP: `GET /api/service-packages` passa a retornar metadados de
  página; consumidores legados que esperam apenas `content` continuam podendo
  ler o mesmo campo, mas devem ignorar ou adotar os novos metadados.
- Testes: integração MockMvc para filtros, páginas, limites, escopo de clínica
  e usuário básico; testes unitários do serviço/repositório quando aplicável.
- Infraestrutura: usa paginação do Spring Data/PostgreSQL, sem migration e sem
  dependência nova.

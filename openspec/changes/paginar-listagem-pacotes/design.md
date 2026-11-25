# Design

## Context

A implementação atual de `GET /api/service-packages` retorna `CatalogResponse`
com todos os registros da clínica e executa os filtros sobre uma lista em
memória. O projeto já possui `PageResponseDTO` e repositórios Spring Data que
usam `Page`, portanto a mudança pode reutilizar o contrato de metadados sem
migration ou dependência nova.

## Goals / Non-Goals

**Goals:**

- Transferir filtro, ordenação e paginação para a consulta JPA/PostgreSQL.
- Manter o comportamento de escopo ADMIN/BASIC antes de executar a consulta.
- Entregar o mesmo envelope paginado consumido pelo frontend irmão.
- Cobrir páginas, filtros, vazio, parâmetros inválidos e isolamento em testes.

**Non-Goals:**

- Alterar criação, detalhe, edição, status, imutabilidade ou modelo persistente.
- Paginar outros catálogos ou modificar `CatalogResponse` globalmente.
- Criar migration, índice novo ou endpoint alternativo.

## Decisions

### Reutilizar `PageResponseDTO`

O controller retornará `PageResponseDTO<ServicePackageResponse>`, usando os
campos já adotados pelo projeto. O `content` permanece com a mesma forma de
`ServicePackageResponse`, reduzindo o impacto para consumidores que já leem
esse campo. Retornar uma lista fatiada manualmente foi descartado porque não
reduziria a leitura em banco nem forneceria `totalElements` confiável.

### Consultar com `Pageable` e filtros opcionais

O repositório terá uma consulta paginada com filtros opcionais por clínica,
unidade, serviço, situação, sessões e nome, ordenada por nome ascendente. A
ordenação será incorporada ao `PageRequest` e a conversão para response será
feita sobre o conteúdo da página.

### Validar limites no serviço

O controller receberá defaults para ausência dos parâmetros e o serviço
validará `page >= 0` e `1 <= size <= 100`, lançando erro de domínio HTTP 400
antes de acessar o repositório. O limite evita solicitações ilimitadas e a
validação no serviço mantém o contrato mesmo para chamadas internas.

### Preservar escopo antes da consulta

Para ADMIN, `unitId` continua sendo validado na clínica autenticada. Para
BASIC, a unidade será substituída pela única unidade autorizada e `active=true`
será forçado, como no comportamento atual. A alternativa de consultar uma
página global e filtrar depois foi descartada por risco de vazamento e por
metadados incorretos.

## Risks / Trade-offs

- [Consumidores antigos não enviam page/size] → defaults preservam a primeira
  página; o campo `content` continua presente.
- [A consulta JPQL com filtros opcionais pode degradar em grandes volumes] →
  manter ordenação determinística, verificar plano posteriormente e registrar
  índices como evolução separada se a medição justificar.
- [A página pode mudar entre solicitações] → aceitar a semântica normal de
  paginação baseada em consulta e manter ordenação por nome estável.
- [O frontend atual tem fallback para envelope legado] → atualizar o contrato
  e os testes integrados após esta API entregar os metadados reais.

## Migration Plan

1. Alterar repositório, serviço e controller para `PageResponseDTO` sem tocar
   no schema ou nas mutations.
2. Atualizar testes MockMvc e documentação do endpoint.
3. Executar `./mvnw test`/`./mvnw verify` conforme o ambiente PostgreSQL
   disponível e validar a change com `--strict`.

Rollback: restaurar o retorno `CatalogResponse<List<...>>` e a consulta atual,
sem rollback de banco, caso um consumidor legado não possa ser atualizado.

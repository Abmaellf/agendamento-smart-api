# Controller de informações do build

## Objetivo do módulo

Expor a versão do artefato em execução.

## Responsabilidades principais

- Consultar `BuildProperties` quando o metadata de build está disponível.
- Usar `ProjectInfoService` como fallback para `project.version`.

## Funcionalidades existentes

- `GET /api/build-info/version` retorna uma `String` com a versão.

## Dependências internas e externas

- Internas: `ProjectInfoService` e `ProjectInfoProperties`.
- Externas: Spring MVC e Spring Boot `BuildProperties`.

## Módulos relacionados

`util`, `service`, `application.properties` e execução do goal `spring-boot:build-info` no Maven.

## Pontos de entrada

- `BuildInfoController.getVersion()`.

## Fluxos de entrada

Requisição autenticada -> `ObjectProvider<BuildProperties>` -> versão do build, ou valor filtrado de `project.version` quando o bean não existe.

## Arquivos críticos

- `BuildInfoController.java`.
- `../../service/ProjectInfoService.java`.
- `../../util/ProjectInfoProperties.java`.

## Regras confirmadas para evolução do módulo

- Informações de build são operacionais e não carregam `tenantId` nem dados de negócio.
- A resposta não pode expor segredos, configuração de banco, tokens ou identificadores pessoais.
- A decisão de tornar versão/health públicos ou autenticados deve ser explícita por ambiente; ela não deve depender apenas do fallback de `anyRequest()`.

## Observações técnicas e débitos identificados

- A rota exige autenticação pela regra padrão; não existe regra explícita ou endpoint de health check.
- O retorno é texto simples, sem DTO de metadata adicional.

# Utilitários de configuração

## Objetivo do módulo

Representar propriedades técnicas do projeto disponibilizadas pelo Spring.

## Responsabilidades principais

- Vincular propriedades com prefixo `project`.
- Expor `project.version` ao `ProjectInfoService`.

## Funcionalidades existentes

- `ProjectInfoProperties` com a propriedade `version`.

## Dependências internas e externas

- Internas: consumido por `ProjectInfoService`.
- Externas: Spring Boot Configuration Properties e Lombok.

## Módulos relacionados

`service`, `controller/info`, `application.properties` e plugin Spring Boot Maven.

## Pontos de entrada

- Binding de configuração na inicialização do contexto.

## Fluxos de entrada

Valor filtrado de `@project.version@` -> environment Spring -> `ProjectInfoProperties` -> service -> endpoint de versão.

## Arquivos críticos

- `ProjectInfoProperties.java`.

## Regras confirmadas para evolução do módulo

- Propriedades técnicas não carregam nem inferem regras de tenant ou permissão.
- Configuração temporal futura deve permitir representação com fuso explícito sem fixar offset no código.
- Valores de build podem ser expostos somente sem segredos ou dados de negócio.

## Observações técnicas e débitos identificados

- A classe está indentada de forma inconsistente.
- Não existe metadata adicional nem validação de propriedade.
- O nome genérico `util` contém uma classe de configuração; um pacote de configuração explícito reduziria ambiguidade.

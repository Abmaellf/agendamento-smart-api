# Infraestrutura

## Objetivo do módulo

Agrupar adaptações técnicas transversais que não pertencem aos fluxos de domínio.

## Responsabilidades principais

- No estado atual, delimitar exclusivamente a infraestrutura de segurança JWT/Spring Security.

## Funcionalidades existentes

- Configuração de segurança web, filtro por requisição e serviço de token no subpacote `security`.

## Dependências internas e externas

- Internas: `repository` e `model/user` por meio do subpacote de segurança.
- Externas: Spring Security e Auth0 Java JWT.

## Módulos relacionados

`controller/auth`, `controller/user`, `service/AuthorizationService` e `infra/security`.

## Pontos de entrada

A cadeia de filtros Spring intercepta requisições; controllers de autenticação invocam emissão de token.

## Fluxos de entrada

Requisição HTTP -> segurança -> controllers; credenciais autenticadas -> emissão de JWT.

## Arquivos críticos

- `security/SecurityConfiguration.java`.
- `security/SecurityFilter.java`.
- `security/TokenService.java`.

## Regras confirmadas para evolução do módulo

- Infraestrutura deve sustentar isolamento por tenant, auditoria, tempo com fuso explícito e execução transacional das invariantes, sem decidir regras de produto.
- Segurança, persistência e relógio devem ser adaptadores testáveis; domínio/use cases não devem depender de detalhes de controller ou de entidade serializada.
- Dados pessoais, tokens, credenciais e motivos de cancelamento/remarcação não podem aparecer em logs técnicos sem política explícita de proteção.

## Observações técnicas e débitos identificados

- Não existem outros adaptadores de infraestrutura além de segurança; persistência está no pacote técnico `repository`.
- A segurança depende diretamente do repository concreto de usuário.
- Consulte `security/README.md` para riscos e regras detalhados.

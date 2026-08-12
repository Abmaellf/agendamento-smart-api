# Controllers transversais

## Objetivo do módulo

Agrupar adaptadores HTTP e hospedar o endpoint técnico de diagnóstico do banco. Os controllers de negócio ficam nos subpacotes.

## Responsabilidades principais

- Expor `GET /debug/db`.
- Delegar autenticação, versão, pacientes, usuários e agendamentos aos submódulos de controller.

## Funcionalidades existentes

- Consulta do nome do banco selecionado pela conexão por `SELECT DATABASE()`.

## Dependências internas e externas

- Internas: subpacotes `auth`, `info`, `patient`, `scheduling` e `user`.
- Externas: Spring MVC e `JdbcTemplate`.

## Módulos relacionados

`infra/security` protege a rota pela regra autenticada padrão; a configuração de datasource fornece a conexão usada por `JdbcTemplate`.

## Pontos de entrada

- `DebugController.db()` — `GET /debug/db`.

## Fluxos de entrada

Uma requisição autenticada atravessa `SecurityFilter`, chega ao controller e executa uma consulta SQL direta, sem service ou repository.

## Arquivos críticos

- `DebugController.java`.

## Regras confirmadas para evolução do módulo

- Controllers são adaptadores: recebem DTOs, obtêm identidade/tenant da sessão e delegam comandos e consultas a services/use cases.
- Nenhum controller pode aceitar `tenantId` ou `clinicId` do cliente como prova de autorização, retornar entidade JPA ou expor senha, token e dados pessoais em logs.
- Rotas técnicas de diagnóstico não podem revelar metadados internos em produção e devem possuir política explícita de exposição.
- Erros de validação, ausência, conflito, capacidade, estado e permissão devem seguir um contrato HTTP comum.

## Observações técnicas e débitos identificados

- Acesso JDBC direto no controller viola o fluxo em camadas predominante.
- A rota revela metadado de infraestrutura e não está condicionada a perfil de desenvolvimento ou papel administrativo.
- Não há tratamento local para indisponibilidade do banco.

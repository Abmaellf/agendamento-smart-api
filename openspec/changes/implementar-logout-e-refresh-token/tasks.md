# Tasks

## 1. Persistência de sessões e configuração

- [x] 1.1 Criar a migration Flyway para sessões de autenticação e tokens de refresh, com hash, usuário, expiração, uso, rotação e revogação, sem alterar tabelas existentes; verificar a aplicação em PostgreSQL descartável e os índices por sessão/hash.
- [x] 1.2 Implementar entidades, repositories e serviço transacional de sessão que armazenem somente hash, vinculem cada sessão a um usuário e detectem refresh ausente, expirado, usado ou revogado; verificar testes de persistência e rejeitar tentativa de usar sessão de outro usuário.
- [x] 1.3 Adicionar configurações de duração, nomes e atributos dos cookies para desenvolvimento e produção; verificar carregamento dos profiles e ausência de segredos nos logs de startup.

## 2. Access JWT e filtro de segurança

- [x] 2.1 Atualizar a emissão do access JWT para incluir identificador de sessão, tipo de token e expiração curta, mantendo o suporte ao cabeçalho `Authorization`; verificar testes unitários de claims, emissor e expiração.
- [x] 2.2 Atualizar o filtro para aceitar somente access JWT do cabeçalho ou cookie `jwt`, validar sessão ativa e rejeitar cookie `refresh_token`, sessão revogada e token expirado; verificar cenários de acesso autorizado e não autorizado sem permitir troca de clínica/usuário.
- [x] 2.3 Atualizar a política de segurança e CORS para liberar somente login, refresh e logout do ciclo público, manter credenciais habilitadas e preservar autorização por papel nos endpoints existentes; verificar testes de autorização e isolamento por clínica.

## 3. Endpoints de login, refresh e logout

- [x] 3.1 Ajustar `POST /auth/login` para criar sessão, emitir `jwt` e `refresh_token` `HttpOnly` com atributos consistentes e devolver DTO sem tokens ou entidade sensível; verificar credenciais válidas/inválidas e os headers `Set-Cookie`.
- [x] 3.2 Implementar `POST /auth/refresh` com rotação atômica, invalidação do valor usado, emissão de novo par de cookies e revogação da cadeia em caso de replay; verificar refresh válido, expirado, ausente, concorrente e reutilizado.
- [x] 3.3 Implementar `POST /auth/logout` idempotente, revogar a sessão quando identificável, expirar ambos os cookies e não exigir access JWT vigente; verificar logout com access expirado, sem cookie, repetido e seguido de tentativa de refresh.
- [x] 3.4 Remover logs de credenciais/tokens e atualizar README do controller e documentação de configuração com o contrato dos endpoints; verificar que respostas, logs e exceções não contêm segredos.

## 4. Verificação integrada do backend

- [x] 4.1 Estender `AuthenticationCookieIntegrationTest` e testes de autenticação para login, refresh, rotação, replay, logout, cookies `Secure`/`HttpOnly`/`SameSite` e acesso após revogação; verificar com `./mvnw test` usando PostgreSQL descartável.
- [x] 4.2 Executar a suíte relacionada a segurança e uma verificação de build (`./mvnw test` e `./mvnw package -DskipTests`), registrar falhas preexistentes ou dependências externas e confirmar que o contrato consumido pelo frontend está documentado.

## Workflow follow-up

- Verificações: `./mvnw test` passou com PostgreSQL descartável; `./mvnw -q -Dtest=AuthenticationCookieIntegrationTest test` passou após cobrir rotação, replay, logout repetido, refresh sem cookie e acesso pós-revogação; `./mvnw -q package -DskipTests` passou.

- Validar a change com `openspec validate implementar-logout-e-refresh-token --strict`.
- Aplicar o backend antes do frontend e arquivar as changes somente após ambos os contratos e verificações estarem concluídos.

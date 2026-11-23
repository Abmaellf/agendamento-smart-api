# Proposal

## Why

A API atualmente emite um JWT de acesso e um cookie `jwt`, mas não oferece refresh token nem logout. Com isso, a aplicação cliente não consegue renovar uma sessão expirada de forma segura e a ação de saída não possui um contrato server-side para revogar a sessão.

## What Changes

- **BREAKING**: substituir a dependência de token de acesso retornado no corpo do login por cookies de sessão `HttpOnly`; o refresh token nunca será devolvido no JSON.
- Fazer `POST /auth/login` criar uma sessão persistida, emitir cookie de acesso de curta duração e cookie de refresh de longa duração.
- Adicionar `POST /auth/refresh` para validar e rotacionar o refresh token, emitindo novos cookies sem exigir um access token ainda válido.
- Adicionar `POST /auth/logout` idempotente para revogar a sessão de refresh e expirar os cookies de acesso e refresh.
- Fazer o filtro de segurança aceitar somente o token de acesso para autenticar recursos protegidos e rejeitar sessões revogadas ou expiradas.
- Criar a persistência e migration necessárias para armazenar apenas o hash do refresh token, sua sessão, expiração, rotação e revogação.
- Cobrir login, refresh, rotação, replay/revogação, logout, cookies e acesso após expiração com testes de integração.

## Capabilities

### New Capabilities

- Nenhuma. O contrato pertence à capability de autenticação e usuários já existente.

### Modified Capabilities

- `authentication-and-users`: alterar o contrato de sessão JWT e adicionar refresh, rotação e logout revogável.

## Impact

- API: `AuthenticationController`, filtros/configuração de segurança, serviço de tokens, persistência de sessões, DTOs/configuração e migration Flyway.
- Contratos HTTP: `POST /auth/login` passa a emitir dois cookies `HttpOnly`; `POST /auth/refresh` e `POST /auth/logout` tornam-se endpoints públicos de sessão, sem retornar segredos no corpo.
- Segurança: cookies devem respeitar `Secure`, `SameSite` e `Path` configuráveis; refresh tokens são armazenados apenas como hash e ficam vinculados ao usuário/sessão.
- Dados: nova tabela de sessões de autenticação; a migration não deve alterar nem apagar dados existentes.
- Fora de escopo: recuperação de senha, MFA, gestão de dispositivos, logout global, alteração de papéis e mudança das regras de isolamento por clínica.

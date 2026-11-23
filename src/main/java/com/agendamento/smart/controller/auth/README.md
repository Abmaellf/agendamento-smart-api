# Controller de autenticação

## Objetivo do módulo

Expor login, renovação, logout, listagem de usuários e consulta do usuário autenticado.

## Responsabilidades principais

- Submeter credenciais ao `AuthenticationManager`.
- Criar sessão persistida e emitir o access JWT no cookie `jwt` e o refresh token opaco no cookie `refresh_token`, ambos HTTP-only.
- Nunca devolver tokens ou a entidade JPA `User` na resposta JSON.
- Consultar usuários e o principal atual.

## Funcionalidades existentes

- `POST /auth/login`: login público.
- `POST /auth/refresh`: rota pública que gira o refresh token por um novo par de cookies; replay, expiração ou ausência revoga a sessão e retorna `401`.
- `POST /auth/logout`: rota pública e idempotente; revoga a sessão identificada pelo refresh cookie e expira os dois cookies.
- `GET /auth/list`: listagem administrativa da própria clínica, somente para `ADMIN`, sem senha/hash e com filtro opcional de unidade.
- `GET /auth/me`: usuário corrente para `ADMIN` ou `USER`, incluindo a unidade persistida e seus nomes de exibição.

## Dependências internas e externas

- Internas: DTOs de autenticação, `TokenService`, `UserService`, `UserRepository` e entidade `User`.
- Externas: Spring MVC, Spring Security e Servlet API.

## Módulos relacionados

- `infra/security`: manager, filtro, token e autorização.
- `model/user` e `repository`: principal persistido.
- `controller/user`: cadastro, que usa uma rota diferente de `/auth/register`.

## Pontos de entrada

- `AuthenticationController.login`.
- `AuthenticationController.list`.
- `AuthenticationController.me`.

## Fluxos de entrada

- Login: credenciais -> `AuthenticationManager` -> sessão + access JWT -> cookies `jwt`/`refresh_token` e `LoginResponseDTO` seguro.
- Refresh: cookie `refresh_token` -> hash persistido com lock -> rotação atômica -> novo access JWT e novo refresh cookie.
- Logout: cookie `refresh_token` -> revogação da sessão quando identificável -> cookies expirados; sem cookie continua `204`.
- Lista: JWT -> autorização `ADMIN` -> validação da clínica/unidade -> DTO seguro, sem serializar entidades `User`.
- Usuário atual: JWT -> `SecurityContext` -> `UserService.currentUserService`.

## Arquivos críticos

- `AuthenticationController.java`.
- `../../infra/security/TokenService.java` e `../../infra/security/SecurityFilter.java`.

## Regras confirmadas para evolução do módulo

- E-mail de acesso é único na plataforma e respostas de login/perfil nunca expõem entidade JPA, senha ou hash.
- A sessão autenticada deve identificar usuário e permanecer revogável; o access JWT carrega o identificador da sessão e a API valida seu estado ativo.
- Contas recém-criadas ficam bloqueadas até a validação do e-mail; usuários com senha temporária devem trocá-la no primeiro acesso.
- O MVP não envia recuperação de senha por e-mail: somente `ADMIN` redefine a senha de usuário do próprio tenant.
- Refresh tokens são persistidos somente como hash SHA-256, com expiração, uso e revogação auditáveis.

## Observações técnicas e débitos identificados

- `LoginResponseDTO` usa `UserLoginResponse`; listagens e perfil usam DTOs seguros e não serializam senha/hash.
- O controller acessa `UserRepository` diretamente, mas limita a clínica autenticada e não retorna senha/hash.
- `JWT_ACCESS_TTL` define a duração padrão de 15 minutos do access cookie e `JWT_REFRESH_TTL` a duração padrão de 30 dias do refresh cookie. `JWT_COOKIE_SECURE` e `JWT_COOKIE_SAME_SITE` controlam os atributos por ambiente.
- A origem `http:localhost:3000` declarada por `@CrossOrigin` está malformada; a configuração global usa `http://localhost:3000`.
- O fluxo de login/refresh/logout não registra credenciais, cookies ou tokens.
- `AuthenticationDTO` não possui constraints, portanto `@Valid` não valida login/senha.

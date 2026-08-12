# Controller de autenticação

## Objetivo do módulo

Expor login, listagem de usuários e consulta do usuário autenticado.

## Responsabilidades principais

- Submeter credenciais ao `AuthenticationManager`.
- Emitir JWT por `TokenService`.
- Gravar o JWT no cookie HTTP-only `jwt`.
- Consultar usuários e o principal atual.

## Funcionalidades existentes

- `POST /auth/login`: login público.
- `GET /auth/list`: listagem completa para `ADMIN` ou `USER`.
- `GET /auth/me`: usuário corrente para `ADMIN` ou `USER`.

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

- Login: credenciais -> `AuthenticationManager` -> JWT -> cookie e `LoginResponseDTO`.
- Lista: JWT -> autorização por papel -> `UserRepository.findAll` -> entidades `User`.
- Usuário atual: JWT -> `SecurityContext` -> `UserService.currentUserService`.

## Arquivos críticos

- `AuthenticationController.java`.
- `../../infra/security/TokenService.java` e `../../infra/security/SecurityFilter.java`.

## Regras confirmadas para evolução do módulo

- E-mail de acesso é único na plataforma e respostas de login/perfil nunca expõem entidade JPA, senha ou hash.
- A sessão autenticada deve identificar usuário, papel, `tenantId`, unidade padrão, validação de e-mail e necessidade de troca de senha.
- Contas recém-criadas ficam bloqueadas até a validação do e-mail; usuários com senha temporária devem trocá-la no primeiro acesso.
- O MVP não envia recuperação de senha por e-mail: somente `ADMIN` redefine a senha de usuário do próprio tenant.
- O logout e a política definitiva de expiração/revogação ainda serão fechados na entrevista.

## Observações técnicas e débitos identificados

- `LoginResponseDTO` e a listagem serializam `User`; `password` não está ignorado na serialização, permitindo exposição do hash.
- O controller acessa `UserRepository` diretamente.
- O cookie tem duração de uma hora, enquanto o token expira em duas horas, e usa `secure=false` em todos os ambientes.
- A origem `http:localhost:3000` declarada por `@CrossOrigin` está malformada; a configuração global usa `http://localhost:3000`.
- Há `System.out.println` no fluxo de login.
- `AuthenticationDTO` não possui constraints, portanto `@Valid` não valida login/senha.

# Infraestrutura de segurança

## Objetivo do módulo

Implementar autenticação e autorização stateless com Spring Security e JWT.

## Responsabilidades principais

- Configurar a `SecurityFilterChain`, CORS, política sem sessão e autorização de rotas.
- Emitir e validar JWT HMAC-SHA256.
- Recuperar token Bearer ou cookie `jwt` e popular o `SecurityContext`.
- Disponibilizar `AuthenticationManager` e `PasswordEncoder`.

## Funcionalidades existentes

- Login público em `/auth/login`.
- Listagem pública de pacientes.
- Autorização por `ADMIN`/`USER` para rotas específicas.
- Autenticação obrigatória como fallback.
- CORS para frontend Vercel e localhost.

## Dependências internas e externas

- Internas: `UserRepository` e entidade `User`.
- Externas: Spring Security, Spring Web, Servlet API e Auth0 Java JWT 4.4.0.

## Módulos relacionados

`controller/auth`, `controller/user`, `service/AuthorizationService`, `model/user` e configuração `api.security.token.secret`.

## Pontos de entrada

- `SecurityConfiguration.securityFilterChain`.
- `SecurityFilter.doFilterInternal` em cada requisição.
- `TokenService.generateToken` e `validateToken`.

## Fluxos de entrada

Header/cookie -> `SecurityFilter` -> validação do JWT -> consulta de usuário -> `SecurityContext` -> autorização por rota/método.

## Arquivos críticos

- `SecurityConfiguration.java`.
- `SecurityFilter.java`.
- `TokenService.java`.

## Regras confirmadas para evolução do módulo

- Toda consulta e comando de negócio deriva `userId`, papel e `tenantId` de uma sessão validada; IDs do request não ampliam o escopo do usuário.
- `ADMIN` e `BASIC` têm permissões diferentes, com nova validação no servidor mesmo quando a interface oculta ações.
- Conta não validada e usuário com troca obrigatória de senha não podem acessar normalmente as rotas internas.
- E-mail de acesso é único na plataforma; tokens e respostas não contêm senha/hash.
- Segredo, atributos de cookie, expiração, logout e eventual revogação devem variar com segurança por ambiente; os detalhes pendentes estão na entrevista.
- Tentativas entre tenants e elevação de papel devem possuir testes de segurança obrigatórios.

## Observações técnicas e débitos identificados

- A regra pública `/auth/register/**` não corresponde ao cadastro real `/user/register/{clinicId}`.
- A regra `GET /api/scheduling` não corresponde à consulta `GET /api/scheduling/{id}`.
- Não há autorização por clínica; o filtro carrega a clínica, mas nenhuma regra a utiliza.
- Se header e cookie estiverem presentes, o cookie prevalece sem explicitar essa política.
- Token inválido retorna subject vazio e ainda provoca busca por login vazio.
- O segredo possui default em `application.properties`.
- Expiração do JWT usa offset fixo `-04:00`; o cookie dura uma hora e o JWT duas.
- Cookie de login usa `secure=false`, inclusive quando acessado pela origem de produção configurada.
- A injeção por campo contrasta com a convenção de construtor recomendada.

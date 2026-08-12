# Controller de usuários

## Objetivo do módulo

Cadastrar um usuário em uma clínica existente.

## Responsabilidades principais

- Verificar duplicidade de login.
- Resolver a clínica informada no path.
- Codificar a senha com BCrypt.
- Mapear e persistir `User`.

## Funcionalidades existentes

- `POST /user/register/{clinicId}` recebe login, senha e papel.

## Dependências internas e externas

- Internas: `UserService`, `UserMapper`, `UserRepository`, `ClinicRepository`, entidades e DTOs.
- Externas: Spring MVC e Spring Security Crypto.

## Módulos relacionados

`controller/auth`, `infra/security`, `model/user`, `model/clinic`, `mapper`, `repository` e `service`.

## Pontos de entrada

- `UserController.createUser(UUID, RegisterDTO)`.

## Fluxos de entrada

JWT válido -> UUID da clínica e corpo -> teste de login -> busca da clínica -> BCrypt -> mapper -> repository -> HTTP 200 vazio.

## Arquivos críticos

- `UserController.java`.
- `../../mapper/UserMapper.java`.
- `../../infra/security/SecurityConfiguration.java`.

## Regras confirmadas para evolução do módulo

- E-mail de acesso é único na plataforma e o tenant do novo usuário deve ser o tenant da sessão, nunca um `clinicId` arbitrário do path.
- `ADMIN` e `BASIC` podem criar usuário `BASIC`; somente `ADMIN` pode criar ou promover outro `ADMIN`.
- O administrador define uma senha inicial temporária; o novo usuário deve trocá-la no primeiro acesso e não pode reutilizá-la.
- Respostas retornam DTO sem senha/hash e registram autoria e timestamps da criação.
- Somente o administrador redefine senha no MVP; recuperação por e-mail está fora do escopo.

## Observações técnicas e débitos identificados

- O controller acessa repositories e concentra regra de negócio que deveria estar em service.
- Qualquer usuário autenticado pode escolher a clínica e enviar `ADMIN` como papel; não há `@PreAuthorize`.
- A liberação configurada para `/auth/register/**` não corresponde a esta rota.
- Um `BCryptPasswordEncoder` é instanciado diretamente, embora exista um bean `PasswordEncoder`.
- O mapper já copia login, papel e clínica, mas o controller repete essas atribuições.
- O retorno declara `UserResponseDTO`, mas responde sem corpo.
- `RegisterDTO` não possui constraints; `@Valid` não valida os campos.
- Há `System.out.println` no cadastro.

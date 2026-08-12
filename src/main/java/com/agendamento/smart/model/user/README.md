# Modelo de usuário

## Objetivo do módulo

Representar o usuário persistido e o principal utilizado pelo Spring Security.

## Responsabilidades principais

- Mapear a tabela `USERS`.
- Armazenar login, senha codificada, papel e clínica.
- Implementar `UserDetails`.
- Converter `UserRole` em authorities Spring.

## Funcionalidades existentes

- `ADMIN` recebe `ROLE_ADMIN` e `ROLE_USER`.
- `USER` recebe `ROLE_USER`.
- Conta, credenciais, bloqueio e habilitação sempre retornam estado válido.
- Associação `ManyToOne` eager e obrigatória com clínica.

## Dependências internas e externas

- Internas: `model/clinic/Clinic`.
- Externas: JPA, Spring Security, Jackson e Lombok.

## Módulos relacionados

`controller/auth`, `controller/user`, `service/AuthorizationService`, `infra/security`, `repository` e migration `V002`.

## Pontos de entrada

- Carregamento por login durante autenticação/filtro.
- Cadastro por `UserController`.
- Serialização direta em login, listagem e usuário atual.

## Fluxos de entrada

Cadastro -> BCrypt -> mapper -> repository; login -> repository -> `UserDetails` -> JWT/SecurityContext.

## Arquivos críticos

- `User.java`.
- `UserRole.java`.

## Regras confirmadas para evolução do módulo

- Os papéis canônicos do tenant são `ADMIN` e `BASIC`; o atual `USER` é legado.
- E-mail/login é único na plataforma e usuário pertence exatamente a um tenant.
- Usuário pode ser vinculado a um profissional, mas as duas entidades permanecem distintas.
- Senha temporária exige troca no primeiro acesso e não pode ser reutilizada; hash nunca é serializado.
- Conta carrega estados necessários para validação de e-mail, ativação e troca obrigatória de senha.
- Apenas `ADMIN` cria/promove administrador e redefine senhas; `ADMIN` e `BASIC` podem criar usuário básico.

## Observações técnicas e débitos identificados

- `password` não possui `@JsonIgnore`; endpoints que retornam a entidade podem expor o hash.
- A entidade JPA é também principal de segurança e contrato HTTP, concentrando três responsabilidades.
- A clínica é carregada eager em toda consulta de usuário.
- Não há auditoria, status de conta ou expiração; métodos de `UserDetails` retornam sempre `true`.
- `@JsonManagedReference` não possui `@JsonBackReference` correspondente em `Clinic`.

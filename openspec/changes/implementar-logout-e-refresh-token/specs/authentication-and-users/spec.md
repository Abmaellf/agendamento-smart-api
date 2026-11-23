# Spec Delta

## MODIFIED Requirements

### Requirement: Login emite JWT e cookie

O sistema MUST aceitar `POST /auth/login` com login e senha, autenticar pelo Spring Security e emitir um cookie de acesso `jwt` e um cookie de refresh `refresh_token`, ambos `HttpOnly`, com atributos de segurança configuráveis. A resposta JSON MUST conter apenas dados públicos da sessão e do usuário, sem access token ou refresh token.

#### Scenario: Credenciais válidas

- **WHEN** um usuário envia credenciais válidas para `POST /auth/login`
- **THEN** recebe resposta de sucesso com dados seguros do usuário, cookie `jwt` de curta duração e cookie `refresh_token` de longa duração, sem tokens no corpo

#### Scenario: Credenciais inválidas

- **WHEN** um usuário envia credenciais inválidas para `POST /auth/login`
- **THEN** a API rejeita a autenticação e não cria uma sessão de refresh válida

### Requirement: Requisições autenticadas aceitam token

O filtro MUST ler o JWT de acesso do cabeçalho `Authorization` ou do cookie `jwt`, validar assinatura, expiração e sessão ativa e carregar o usuário pelo login. O cookie `jwt` prevalece quando ambos são fornecidos; o cookie `refresh_token` MUST NOT autenticar recursos protegidos.

#### Scenario: Token válido

- **WHEN** uma requisição protegida contém JWT de acesso válido associado a uma sessão ativa
- **THEN** o principal é carregado e a requisição segue autenticada

#### Scenario: Token de acesso expirado ou sessão revogada

- **WHEN** uma requisição protegida contém JWT expirado ou associado a sessão revogada
- **THEN** a API rejeita a requisição como não autenticada

## ADDED Requirements

### Requirement: A API renova a sessão por refresh token rotativo

`POST /auth/refresh` MUST validar o cookie `refresh_token`, invalidar o valor usado e emitir um novo par de cookies para a mesma sessão e usuário, sem exigir um JWT de acesso válido.

#### Scenario: Refresh válido

- **WHEN** `POST /auth/refresh` recebe um refresh token vigente e não utilizado
- **THEN** responde com sucesso, substitui o cookie `refresh_token`, substitui o cookie `jwt` e não devolve token em JSON

#### Scenario: Refresh inválido ou reutilizado

- **WHEN** `POST /auth/refresh` recebe token ausente, expirado, inválido ou já rotacionado
- **THEN** rejeita a chamada, revoga a sessão relacionada quando ela puder ser identificada e expira os cookies de sessão

### Requirement: O logout revoga a sessão e expira os cookies

`POST /auth/logout` MUST ser idempotente, revogar a sessão identificada pelo refresh token quando presente e expirar os cookies `jwt` e `refresh_token` com os mesmos atributos de escopo.

#### Scenario: Logout com sessão vigente

- **WHEN** `POST /auth/logout` recebe uma sessão válida
- **THEN** responde com sucesso, revoga a sessão, expira os dois cookies e impede novo refresh com o token anterior

#### Scenario: Logout sem sessão vigente

- **WHEN** `POST /auth/logout` recebe cookies ausentes, expirados ou já revogados
- **THEN** responde de forma idempotente, expira os cookies e não cria uma nova sessão

### Requirement: Segredos de sessão não aparecem em respostas ou logs

A API MUST armazenar apenas representação não reversível do refresh token e MUST NOT registrar access token, refresh token, senha ou seus valores em respostas, logs ou mensagens de erro.

#### Scenario: Criação e rotação de sessão

- **WHEN** login ou refresh criar ou rotacionar uma sessão
- **THEN** a persistência e os registros operacionais não contêm o valor bruto do refresh token

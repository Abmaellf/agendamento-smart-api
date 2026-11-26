# Spec Delta

## Purpose

Define as origens web autorizadas pela API e o comportamento CORS necessário para que front-ends publicados e ambientes locais consumam endpoints públicos e autenticados com segurança.

## ADDED Requirements

### Requirement: O front-end Compromiz é uma origem CORS autorizada

A API MUST aceitar requisições originadas exatamente em `https://compromiz.com.br`, sem barra final, incluindo preflight e requisições autenticadas com credenciais.

#### Scenario: Preflight do front-end Compromiz

- **WHEN** o navegador envia uma requisição `OPTIONS` com `Origin: https://compromiz.com.br` para um endpoint da API e solicita um método permitido
- **THEN** a resposta informa `Access-Control-Allow-Origin: https://compromiz.com.br`, permite credenciais e não bloqueia o método ou os cabeçalhos solicitados quando estes estiverem dentro da política CORS

#### Scenario: Requisição autenticada do front-end Compromiz

- **WHEN** o front-end Compromiz envia uma requisição real com `Origin: https://compromiz.com.br` e credenciais de sessão
- **THEN** a API processa a requisição normalmente, sujeita às regras de autenticação e autorização, e devolve os cabeçalhos CORS compatíveis com essa origem

### Requirement: As origens CORS existentes permanecem compatíveis

A API MUST continuar aceitando `https://agendamentos-smart.vercel.app`, `http://localhost:3000` e `http://localhost:5173` com a mesma política de métodos, cabeçalhos e credenciais vigente antes desta mudança.

#### Scenario: Front-end Vercel continua autorizado

- **WHEN** o front-end Vercel envia um preflight ou uma requisição real com sua origem atualmente suportada
- **THEN** a API mantém a resposta CORS permitida e o fluxo de autenticação existente não é removido

#### Scenario: Ambiente local continua autorizado

- **WHEN** um front-end local em `http://localhost:3000` ou `http://localhost:5173` envia uma requisição CORS
- **THEN** a API mantém a resposta CORS permitida conforme a política atual

### Requirement: Origens não autorizadas não recebem acesso CORS

A API MUST permitir somente as origens explicitamente autorizadas e MUST NOT usar `*` ou padrões equivalentes para liberar o novo domínio.

#### Scenario: Origem externa não autorizada

- **WHEN** um navegador envia preflight ou requisição real com uma origem diferente das origens autorizadas
- **THEN** a resposta não concede `Access-Control-Allow-Origin` para essa origem nem habilita credenciais CORS

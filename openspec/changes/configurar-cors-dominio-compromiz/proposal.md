# Proposal

## Why

O front-end publicado em `https://compromiz.com.br` não está entre as origens permitidas pelas configurações CORS atuais da API. Como a autenticação pode usar cookies e requisições cross-origin, o navegador bloqueia o preflight e as chamadas do novo front-end até que essa origem seja autorizada de forma consistente.

## What Changes

- Adicionar exatamente `https://compromiz.com.br` às origens CORS permitidas pela API.
- Atualizar a configuração global de CORS e as declarações específicas dos endpoints que possuem `@CrossOrigin`, mantendo as origens locais e a origem Vercel já suportadas.
- Preservar os métodos, cabeçalhos, credenciais e o cache de preflight já configurados, incluindo `Access-Control-Allow-Credentials: true` para o fluxo de autenticação por cookie.
- Validar que preflight e requisições reais originadas em `https://compromiz.com.br` recebem os cabeçalhos CORS esperados.
- Fora de escopo: alteração de domínio da API, proxy/TLS, política de cookies, autenticação/autorização, remoção das origens já suportadas ou habilitação de curingas de origem.

## Capabilities

### New Capabilities

- `cors-origins`: controle das origens web autorizadas e do comportamento CORS da API para clientes locais e front-ends publicados.

### Modified Capabilities

- Nenhuma.

## Impact

- Configuração de segurança Spring em `SecurityConfiguration`.
- Declarações CORS em `AuthenticationController` e `UserController`.
- Testes de integração ou de configuração necessários para verificar preflight, credenciais e rejeição de origens não autorizadas.
- Documentação de deploy/integração do front-end, caso o procedimento atualmente cite apenas o domínio Vercel.

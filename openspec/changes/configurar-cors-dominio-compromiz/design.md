# Design

## Context

Consulte `proposal.md` para a motivação. A API já habilita CORS globalmente e declara a lista de origens em `SecurityConfiguration`; `AuthenticationController` e o endpoint de cadastro de usuário também possuem declarações específicas. As listas atuais não incluem `https://compromiz.com.br`, portanto a alteração precisa manter esses pontos coerentes.

## Goals / Non-Goals

**Goals:**

- Autorizar a origem HTTPS exata do front-end Compromiz em endpoints globais e nos endpoints com política CORS específica.
- Preservar métodos, cabeçalhos, credenciais e origens atualmente suportados.
- Demonstrar por testes que preflight, requisições reais e rejeição de origem externa seguem o contrato.
- Atualizar o guia operacional quando ele mencionar a origem pública anterior como única origem de produção.

**Non-Goals:**

- Não alterar a política de `SameSite`/`Secure` dos cookies, o fluxo JWT, a autorização de rotas ou o domínio da API.
- Não transformar a lista em configuração aberta, wildcard ou regra baseada em sufixo de domínio.
- Não remover as origens locais nem a origem Vercel durante esta change.

## Decisions

### Manter a lista explícita de origens

A origem será adicionada como valor exato `https://compromiz.com.br`, sem barra final e sem curinga. Essa escolha preserva a proteção contra origens arbitrárias e é compatível com `allowCredentials=true`.

Alternativas consideradas: `*` ou padrões amplos foram rejeitados porque concederiam acesso além do front-end autorizado e são incompatíveis com credenciais; aceitar também variantes HTTP ou `www` foi rejeitado porque não foram solicitadas e são origens diferentes.

### Atualizar os pontos de política existentes

A mesma origem será refletida na política global e nas declarações específicas de autenticação e cadastro de usuário. Isso evita que uma rota funcione para o novo front-end enquanto outra seja bloqueada por uma anotação mais restritiva.

Alternativa considerada: remover as declarações específicas e depender somente da política global. Essa refatoração reduziria duplicação, mas mudaria a estrutura de segurança sem ser necessária para o objetivo desta change; fica para uma mudança posterior, se desejado.

### Validar com preflight e requisição real

Os testes devem exercitar `OPTIONS` com `Origin` e cabeçalhos de solicitação, verificar `Access-Control-Allow-Origin` e `Access-Control-Allow-Credentials`, e confirmar que uma origem externa não recebe esses privilégios. Deve haver cobertura dos endpoints de autenticação e cadastro, além de pelo menos uma rota protegida atendida pela política global.

## Risks / Trade-offs

- [As listas explícitas podem divergir no futuro] → manter um teste que verifique a origem Compromiz nos fluxos global e específico; uma refatoração para propriedade compartilhada pode ser considerada depois.
- [O cookie de sessão pode continuar sujeito às regras de SameSite em cenários cross-site] → manter essa questão fora do escopo CORS e validar separadamente a estratégia de cookies no ambiente HTTPS.
- [O guia de deploy pode ficar desatualizado] → atualizar exemplos de `Origin` e preflight para o domínio Compromiz sem afirmar que a validação foi executada quando apenas o teste automatizado foi rodado.

## Migration Plan

1. Atualizar as listas de origens e a documentação de integração.
2. Executar os testes CORS e a suíte Maven relevante.
3. Publicar a API e validar o preflight contra a URL HTTPS real da API com `Origin: https://compromiz.com.br`.
4. Em rollback, restaurar a versão anterior da aplicação; isso remove o acesso CORS do novo front-end sem alterar dados ou migrations.

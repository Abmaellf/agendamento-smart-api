# Tasks

## 1. Atualizar as políticas CORS

- [x] 1.1 Adicionar exatamente `https://compromiz.com.br` à lista global de origens permitidas, preservando `https://agendamentos-smart.vercel.app`, `http://localhost:3000` e `http://localhost:5173`; verificar por inspeção da configuração que métodos, cabeçalhos, `allowCredentials` e `maxAge` não foram alterados.
- [x] 1.2 Atualizar as declarações CORS específicas dos endpoints de autenticação e cadastro de usuário para aceitar a mesma origem Compromiz; verificar que preflight desses endpoints não diverge da política global.

## 2. Cobrir o contrato com testes

- [x] 2.1 Criar ou ajustar testes para preflight e requisição real com `Origin: https://compromiz.com.br` em rota de autenticação, cadastro de usuário e rota protegida pela política global; verificar `Access-Control-Allow-Origin` exato e `Access-Control-Allow-Credentials: true`, mantendo autenticação e autorização da rota.
- [x] 2.2 Cobrir a compatibilidade das três origens existentes e a rejeição de uma origem externa não autorizada; verificar que nenhum cenário usa wildcard para conceder credenciais.

## 3. Atualizar a integração operacional

- [x] 3.1 Atualizar `docs/GUIA_DEPLOY_E_INTEGRACAO_FRONTEND.md` para documentar `https://compromiz.com.br` como origem pública autorizada e ajustar exemplos de preflight/diagnóstico; verificar com busca textual que os comandos usam a origem exata sem barra final.

## 4. Validar a change

- [x] 4.1 Executar os testes Maven relevantes e `./mvnw clean verify`; verificar que a suíte termina com sucesso e registrar qualquer limitação de ambiente sem afirmar validação integrada não executada.
- [x] 4.2 Executar `/home/abmael/.nvm/versions/node/v22.14.0/bin/openspec validate configurar-cors-dominio-compromiz --strict`; verificar que todos os artefatos e cenários da change são válidos.

## Workflow follow-up

- Arquivar a change somente após a implementação, revisão e validação da API estarem concluídas.
- Sincronizar a spec principal `cors-origins` ao arquivar a change, preservando os artefatos desta mudança no histórico.

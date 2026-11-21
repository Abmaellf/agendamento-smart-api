# Tasks

## 1. Topologia Docker de produção

- [x] 1.1 Criar o Compose de produção com os serviços `api`, `postgres` e `proxy`, redes `edge`/`backend`, volumes persistentes do proxy e imagem do proxy fixada por versão ou digest; verificar com `docker compose -f compose.production.yml config --quiet` e confirmar que o arquivo é válido sem alterar o volume do PostgreSQL.
- [x] 1.2 Remover os mapeamentos de host da API e do PostgreSQL no cenário de produção e publicar somente 80/443 no proxy; verificar com `docker compose -f compose.production.yml config` que não existem portas publicadas para 8080/5432.
- [x] 1.3 Adicionar a configuração do proxy para o domínio fornecido pelo ambiente, encaminhar para `api:8080`, redirecionar HTTP para HTTPS e persistir dados/configuração TLS; verificar a sintaxe com a validação disponível na imagem e revisar que o upstream não usa `localhost`.

## 2. Integração da aplicação e segurança de borda

- [x] 2.1 Configurar `server.forward-headers-strategy=framework` somente no profile `prod` e preservar `JWT_COOKIE_SECURE=true` nesse ambiente; verificar a configuração efetiva sem alterar os profiles `test` e `homo`.
- [x] 2.2 Criar ou ajustar testes de integração do login através da entrada HTTPS para confirmar cookie JWT `Secure`/`HttpOnly` e requisição autenticada posterior; verificar também que o token e as regras de autorização existentes continuam funcionando.

## 3. Operação e documentação

- [x] 3.1 Documentar domínio DNS, firewall, variáveis de ambiente, volumes de certificados, comando de subida, renovação e rollback no guia de deploy; verificar que os comandos documentados apontam para o Compose de produção e não expõem segredos.
- [x] 3.2 Atualizar o guia de integração do front-end para usar a URL HTTPS final e registrar a distinção entre o Compose local HTTP e o deploy público HTTPS; verificar que a origem CORS e o uso de `credentials` estão descritos sem inventar um domínio real.

## 4. Validação integrada

- [x] 4.1 Executar `./mvnw clean verify` e validar a configuração dos arquivos Compose; registrar os resultados observados e eventuais limitações de ambiente.
- [ ] 4.2 Com DNS e firewall reais configurados, executar smoke tests externos para confirmar redirecionamento 80→443, certificado válido, roteamento HTTPS até a API, login por cookie e inacessibilidade das portas 8080/5432; verificar no `docker compose ps` que somente o proxy publica portas no host.

Resultado observado em 2026-11-20: `docker compose -f compose.production.yml
config --quiet` passou, assim como a validação do Caddyfile e o build/compilação.
`./mvnw clean verify` executou 63 testes, com 62 aprovados e uma falha
preexistente em `ClinicStructureEndToEndIntegrationTest`, que espera HTTP 200
onde o endpoint atual responde HTTP 201. O teste novo
`AuthenticationCookieIntegrationTest` passou.

## Workflow follow-up

- Validar a change com `openspec validate adicionar-proxy-reverso-https-docker --strict` antes de implementar.
- Após a implementação e revisão, sincronizar a spec principal e arquivar a change conforme o fluxo OpenSpec.


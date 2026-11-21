# Proposal

## Why

A API atualmente publica HTTP diretamente na porta 8080 pelo Compose, enquanto o
deploy documentado exige acesso público somente por HTTPS. Isso deixa a entrada
de produção sem terminação TLS, permite contornar uma futura camada de entrada e
é incompatível com o consumo seguro pelo front-end publicado.

O Docker Compose já fornece a rede necessária para separar a borda da API e do
PostgreSQL. A mudança formaliza um proxy reverso HTTPS containerizado, com Caddy
como opção adotada para terminação TLS e renovação automática de certificados.

## What Changes

- Adicionar um serviço de proxy reverso HTTPS ao cenário Docker, encaminhando o
  domínio público para `api:8080` pela rede interna.
- Publicar no host somente as portas 80 e 443 pelo proxy; remover a publicação
  direta das portas 8080 da API e 5432 do PostgreSQL no cenário de produção.
- Persistir certificados e estado do proxy em volumes Docker e fixar a imagem do
  proxy por versão ou digest homologado.
- Redirecionar HTTP para HTTPS e preservar os cabeçalhos encaminhados necessários
  para que o Spring reconheça o esquema, host e IP originais.
- Manter `JWT_COOKIE_SECURE=true` no profile de produção e exigir configuração de
  domínio/DNS, CORS e firewall antes da exposição pública.
- Incluir validações de configuração, roteamento, TLS, persistência dos
  certificados e inacessibilidade externa das portas internas.

## Capabilities

### New Capabilities

- `https-reverse-proxy`: entrada HTTPS da API, topologia Docker de borda, TLS,
  encaminhamento para a aplicação e isolamento das portas internas.

### Modified Capabilities

- Nenhuma. O login e a validação dos tokens permanecem no backend; a mudança
  apenas disponibiliza a entrada segura e usa a configuração de cookie seguro já
  existente no profile `prod`.

## Impact

- `docker-compose.yml`: novo serviço, rede de borda, volumes persistentes e
  remoção dos mapeamentos públicos da API e do banco no cenário de produção.
- Novo `Caddyfile` ou configuração equivalente do proxy, com domínio fornecido
  pelo ambiente de deploy.
- Configuração Spring de produção para interpretar cabeçalhos `X-Forwarded-*`.
- Documentação de deploy, DNS, firewall, variáveis de ambiente e smoke tests.
- A publicação pública passa a depender de um domínio apontado para o servidor,
  portas 80/443 liberadas e armazenamento persistente dos certificados. O
  ambiente local/teste deve continuar podendo acessar a API por HTTP sem exigir
  ACME ou DNS público.


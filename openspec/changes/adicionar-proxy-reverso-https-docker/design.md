# Design

## Context

Consulte `proposal.md` para a motivação e `specs/https-reverse-proxy/spec.md`
para o contrato. O Compose atual possui somente `api` e `postgres`, publica
8080 e 5432 no host e usa HTTP interno na API. O profile `prod` já permite
`JWT_COOKIE_SECURE=true`, mas a aplicação ainda precisa interpretar o contexto
HTTPS terminado fora do processo Java. O repositório também documenta que não
possui um Compose de produção, um `Caddyfile` ou certificados.

## Goals / Non-Goals

**Goals:**

- Entregar uma topologia Docker de produção com uma única entrada pública em
  80/443.
- Terminar TLS no proxy, encaminhar para `api:8080` e manter o PostgreSQL fora
  da rede de borda.
- Persistir certificados e estado do proxy, com imagem fixada e logs operáveis.
- Preservar o Compose local/teste e seu acesso HTTP em `localhost:8080`.
- Fazer o Spring reconhecer `X-Forwarded-Proto`, `X-Forwarded-Host` e
  `X-Forwarded-For` somente no profile de produção.

**Non-Goals:**

- Não alterar os endpoints, o formato do JWT, a política de autorização ou o
  modelo de dados.
- Não criar neste change o bootstrap de DNS, firewall do provedor, conta ACME
  ou gerenciamento externo de segredos.
- Não adicionar um endpoint de healthcheck de negócio; isso pode ser uma change
  posterior, embora o proxy e o Compose devam ser validados operacionalmente.
- Não remover o modo HTTP local/teste nem tornar certificados públicos
  obrigatórios para desenvolvimento.

## Decisions

### Compose de produção separado

Será adicionado um `compose.production.yml` (nome a confirmar na implementação)
para descrever a topologia pública sem alterar o `docker-compose.yml` usado pelo
desenvolvimento. O arquivo de produção reutiliza a mesma imagem/build da API,
mas não publica `api` nem `postgres`; o operador o executa junto das variáveis
de produção e de um arquivo de configuração do proxy.

Isso evita que uma máquina local exija domínio público e ACME, e torna explícita
a diferença entre teste HTTP e publicação HTTPS. Alterar o Compose atual com
profiles ou tags de merge é uma alternativa possível, mas aumenta o risco de
um deploy local publicar portas ou iniciar o proxy sem DNS configurado.

### Caddy como terminador TLS

O proxy será Caddy, com imagem fixada por versão ou digest previamente
homologado. Ele será configurado para o domínio fornecido pelo ambiente,
redirecionar HTTP para HTTPS e fazer `reverse_proxy api:8080`. Caddy é escolhido
porque cobre ACME e renovação automática sem exigir um contêiner separado de
Certbot; Nginx + Certbot e Traefik permanecem alternativas compatíveis com o
contrato, mas exigem mais artefatos ou configuração operacional.

O domínio e o e-mail/opções de ACME não serão gravados como segredo na imagem.
O `Caddyfile` poderá receber o domínio por variável ou ser fornecido como
arquivo de deploy fora do repositório, conforme a política do ambiente.

### Redes e volumes

Serão usadas duas redes bridge:

```text
Internet -> proxy (edge: 80/443) -> api (edge: 8080)
                                      |
                                      +-> postgres (backend: 5432)
```

O proxy participa somente de `edge`; a API participa de `edge` e `backend`; o
PostgreSQL participa somente de `backend`. Os volumes nomeados do proxy
armazenarão dados de certificados e configuração/runtime. O volume existente do
PostgreSQL será preservado e não será reutilizado pelo proxy.

### Cabeçalhos encaminhados e cookie

O proxy será a única entrada pública, portanto os cabeçalhos encaminhados que
ele envia não poderão ser falsificados por uma rota externa direta à API. No
profile `prod`, `server.forward-headers-strategy=framework` fará o Spring
interpretar o esquema e host originais. A configuração existente de
`JWT_COOKIE_SECURE` continuará sendo explicitamente `true` no ambiente de
produção; `test` e `homo` permanecerão adequados ao HTTP local.

### Observabilidade e validação

O serviço do proxy terá política de reinício, `no-new-privileges` quando
compatível com a imagem e rotação de logs equivalente aos serviços existentes.
Antes do deploy, `docker compose config` validará a composição. Depois, os
testes deverão confirmar o redirecionamento 80→443, certificado válido, acesso
HTTPS a uma rota da API, cookie seguro no login e ausência de exposição direta
de 8080/5432 a partir de uma máquina externa.

## Risks / Trade-offs

- [DNS ou portas 80/443 indisponíveis] → ACME não consegue emitir/renovar o
  certificado; validar DNS público e firewall antes de subir o proxy e manter o
  acesso privado durante a preparação.
- [Volume de certificados removido] → o proxy perde estado e pode atingir
  limites de emissão; preservar `caddy_data` e `caddy_config` em atualizações.
- [Imagem do proxy com tag flutuante] → comportamento ou vulnerabilidades podem
  mudar sem revisão; exigir versão imutável ou digest homologado.
- [API ainda publicada pelo Compose] → clientes contornam TLS e enviam tráfego
  HTTP; verificar `docker compose ps` e remover os mapeamentos de `api` e
  `postgres` no arquivo de produção.
- [CORS ou cookie cross-site incompatível com o domínio final] → o TLS pode
  funcionar, mas o front não manter sessão; validar a origem real do front e
  manter a decisão de `SameSite` como revisão explícita, fora deste proxy.

## Migration Plan

1. Fixar a versão/digest da imagem do proxy, preparar o domínio DNS e liberar
   somente 80/443 no firewall.
2. Adicionar o Compose de produção, configuração do proxy, redes, volumes e
   `forward-headers` no profile `prod`; validar a configuração sem remover o
   volume do PostgreSQL.
3. Subir o proxy com a API e o banco, aguardar a emissão do certificado e
   executar os smoke tests externos.
4. Confirmar que `api` e `postgres` não possuem portas publicadas e que o
   front usa a URL HTTPS final.
5. Para rollback, parar o proxy e restaurar a versão anterior do arquivo de
   produção/imagem, mantendo os volumes de certificados e banco. Não reabrir
   8080 ou 5432 publicamente; se for necessário diagnóstico, usar loopback ou
   uma rede administrativa controlada.


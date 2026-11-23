# Especificação e registro da implementação do proxy reverso HTTPS

## Identificação e estado

- **Change OpenSpec:** `adicionar-proxy-reverso-https-docker`
- **Capability:** `https-reverse-proxy`
- **Implementação inicial:** commit `70701d2` — 20/11/2026
- **Última atualização registrada neste documento:** 21/11/2026
- **Estado:** implementação local concluída e validada; smoke test com DNS/firewall reais ainda pendente
- **Arquivos OpenSpec:** [proposal](../openspec/changes/adicionar-proxy-reverso-https-docker/proposal.md), [design](../openspec/changes/adicionar-proxy-reverso-https-docker/design.md), [spec delta](../openspec/changes/adicionar-proxy-reverso-https-docker/specs/https-reverse-proxy/spec.md) e [tasks](../openspec/changes/adicionar-proxy-reverso-https-docker/tasks.md)

Este documento consolida a intenção original da change, o desenho técnico, o que foi implementado no repositório, as validações executadas e as alterações feitas depois da especificação inicial.

## 1. Motivação

Antes da change, a API era publicada diretamente por HTTP na porta `8080`. Esse modelo não fornecia terminação TLS, permitia contornar uma futura camada de entrada e não atendia ao requisito de consumo público seguro pelo front-end.

A solução adotada adiciona um proxy reverso HTTPS containerizado com Caddy. O proxy é a única entrada pública da implantação de produção, termina TLS, redireciona HTTP para HTTPS e encaminha as requisições internamente para `api:8080`.

## 2. Escopo da especificação original

A change definiu os seguintes objetivos:

1. Adicionar um serviço de proxy HTTPS ao Docker Compose de produção.
2. Publicar no host somente as portas `80` e `443`.
3. Remover a publicação direta de `8080` da API e `5432` do PostgreSQL em produção.
4. Persistir certificados e estado do proxy em volumes Docker.
5. Fixar a imagem do proxy por versão ou digest homologado.
6. Redirecionar HTTP para HTTPS.
7. Encaminhar o contexto original da requisição para o Spring.
8. Manter `JWT_COOKIE_SECURE=true` no profile `prod`.
9. Preservar o Compose local HTTP, sem exigir DNS público ou ACME.
10. Documentar DNS, firewall, variáveis, deploy, renovação, rollback, CORS e integração com o front-end.

Não fazia parte do escopo alterar endpoints, formato do JWT, regras de autorização, modelo de dados, bootstrap de DNS, firewall do provedor ou gerenciamento externo de segredos.

## 3. Requisitos e cenários

### 3.1 A API pública usa HTTPS

O domínio público de produção deve responder por HTTPS válido. A porta `80` deve redirecionar para a mesma URL em HTTPS, sem oferecer o fluxo funcional da API por HTTP.

**Cenários:**

- Cliente acessa o domínio na porta `80` → recebe redirecionamento para a porta `443`, preservando o recurso.
- Cliente acessa uma rota pelo domínio HTTPS → o proxy encaminha para a API sem expor a porta interna `8080`.

### 3.2 As portas internas ficam isoladas

Somente o serviço `proxy` pode possuir mapeamentos de portas para o host. A API e o PostgreSQL devem ser acessíveis apenas pelas redes Docker internas.

**Cenários:**

- Inspeção do Compose → somente `80:80` e `443:443` aparecem no serviço `proxy`.
- Tentativa externa em `8080` ou `5432` → não alcança a API nem o banco.

### 3.3 Certificados e estado TLS são persistentes

O proxy deve obter e renovar automaticamente o certificado quando o DNS e o firewall permitirem. Certificados e estado TLS devem sobreviver à recriação do contêiner, desde que os volumes não sejam removidos.

### 3.4 O backend reconhece a requisição original

O Spring deve interpretar o esquema, host e demais cabeçalhos encaminhados pelo proxy. O login em produção deve emitir cookie JWT com `Secure` e `HttpOnly`, utilizável em uma requisição autenticada posterior pela origem pública.

### 3.5 O modo local não depende de ACME

O Compose local/teste deve continuar permitindo acesso HTTP em `localhost:8080`, sem domínio público, certificado ACME, proxy ou portas `80`/`443`.

## 4. Desenho técnico adotado

### 4.1 Topologia

```text
Internet
   |
   | 80/443
   v
proxy (Caddy, rede edge)
   |
   | reverse_proxy api:8080
   v
api (redes edge + backend)
   |
   | JDBC interno:5432
   v
postgres (rede backend)
```

- `proxy` participa somente da rede `edge`.
- `api` participa das redes `edge` e `backend`.
- `postgres` participa somente da rede `backend`.
- O proxy publica `80` e `443`; `api` e `postgres` não publicam portas no host.
- O volume existente do PostgreSQL é preservado.
- `caddy_data` mantém certificados e estado operacional.
- `caddy_config` mantém o estado de configuração do Caddy.

### 4.2 Escolha do Caddy

Caddy foi escolhido por fornecer terminação TLS e ACME/renovação automática sem exigir um contêiner separado de Certbot. A imagem implementada é `caddy:2.9.1-alpine`; a validação local também registrou o digest:

```text
sha256:b4e3952384eb9524a887633ce65c752dd7c71314d2c2acf98cd5c715aaa534f
```

Nginx com Certbot e Traefik continuam sendo alternativas possíveis, mas não foram adotados nesta change.

## 5. Implementação no repositório

### 5.1 Compose de produção

O arquivo [compose.production.yml](../compose.production.yml) contém os serviços `api`, `postgres` e `proxy`.

O serviço `api`:

- constrói a imagem a partir do `Dockerfile`;
- exige `APP_VERSION`, `SPRING_PROFILES_ACTIVE`, credenciais do banco e `JWT_SECRET`;
- expõe somente `8080` dentro da rede Docker, sem `ports` para o host;
- usa `read_only`, `/tmp` em `tmpfs`, `no-new-privileges`, reinício automático e rotação de logs;
- participa das redes `backend` e `edge`.

O serviço `postgres`:

- usa PostgreSQL `18`;
- mantém o volume `postgresql_data`;
- possui healthcheck com `pg_isready`;
- participa somente da rede `backend`;
- não publica `5432` no host.

O serviço `proxy`:

- usa `caddy:2.9.1-alpine`;
- exige `API_DOMAIN` e `ACME_EMAIL`;
- publica `80:80` e `443:443`;
- monta o [Caddyfile](../Caddyfile) como somente leitura;
- persiste `caddy_data` e `caddy_config`;
- usa `read_only`, `/tmp` em `tmpfs`, `no-new-privileges`, reinício automático e rotação de logs;
- participa somente da rede `edge`.

### 5.2 Configuração do Caddy

O arquivo [Caddyfile](../Caddyfile) recebe o domínio e o e-mail por ambiente:

```caddyfile
{
    email {$ACME_EMAIL}
}

http://{$API_DOMAIN} {
    redir https://{$API_DOMAIN}{uri} permanent
}

https://{$API_DOMAIN} {
    reverse_proxy api:8080
}
```

O upstream é `api:8080`, nunca `localhost`, porque o proxy e a API são contêineres distintos.

### 5.3 Integração Spring e cookies

O profile [application-prod.yaml](../src/main/resources/application-prod.yaml) contém:

```yaml
server:
  forward-headers-strategy: framework

api:
  security:
    cookie:
      secure: ${JWT_COOKIE_SECURE:true}
      same-site: ${JWT_COOKIE_SAME_SITE:None}
```

Assim, o Spring reconhece o contexto HTTPS original terminado pelo proxy, enquanto o cookie JWT permanece seguro por padrão em produção. Os profiles `test` e `homo` continuam independentes dessa exigência.

O controlador de autenticação lê `api.security.cookie.same-site` e aplica o valor ao cookie. O cookie de login também permanece `HttpOnly`, tem caminho `/` e validade de uma hora.

### 5.4 Teste de integração

Foi adicionado [AuthenticationCookieIntegrationTest.java](../src/test/java/com/agendamento/smart/controller/auth/AuthenticationCookieIntegrationTest.java), com `@ActiveProfiles("prod")`. O teste:

1. registra uma clínica;
2. executa login;
3. verifica status `200`;
4. verifica `Secure` e `HttpOnly` no `Set-Cookie`;
5. reutiliza o cookie em `/auth/me` e espera `200`.

O suporte de integração PostgreSQL deixou de forçar `api.security.cookie.secure=false`, permitindo testar a configuração real do profile de produção.

Esse teste valida o comportamento da aplicação com o profile `prod`; ele não substitui o smoke test de rede através de um domínio e de um contêiner Caddy reais.

## 6. Configuração operacional

### 6.1 Variáveis exigidas

O ambiente de produção deve fornecer, sem registrar segredos no repositório:

```dotenv
APP_VERSION=<versao-ou-sha-imutavel>
SPRING_PROFILES_ACTIVE=prod
AGENDA_DB_NAME=agendamento-smart-api
AGENDA_DB_USER=<usuario-do-postgres>
AGENDA_DB_PASSWORD=<senha-do-postgres>
JWT_SECRET=<segredo-longo-e-exclusivo>
JWT_COOKIE_SECURE=true
API_DOMAIN=api.seu-dominio.com
ACME_EMAIL=ops@seu-dominio.com
```

`API_DOMAIN` deve apontar por DNS para o servidor. O firewall deve permitir entrada em `80/tcp` e `443/tcp`; `8080/tcp` e `5432/tcp` não devem ser liberadas.

### 6.2 Subida e validação

Os comandos documentados usam o arquivo de ambiente protegido e o Compose de produção:

```bash
docker compose --env-file /etc/agendamento-smart/agendamento-smart.prod.env -f compose.production.yml config --quiet
docker compose --env-file /etc/agendamento-smart/agendamento-smart.prod.env -f compose.production.yml up -d --build
docker compose --env-file /etc/agendamento-smart/agendamento-smart.prod.env -f compose.production.yml ps
```

Smoke tests esperados:

```bash
curl -I http://api.seu-dominio.com/actuator/health
curl -i https://api.seu-dominio.com/actuator/health
nc -vz api.seu-dominio.com 80
nc -vz api.seu-dominio.com 443
```

Também devem ser confirmados o certificado válido, o redirecionamento `80 → 443`, o login com cookie e a ausência de exposição de `8080` e `5432` a partir de uma máquina externa.

Não se deve usar `docker compose down -v` em uma atualização normal, pois isso removeria volumes persistentes. Rollback deve manter os volumes do PostgreSQL e do Caddy.

### 6.3 Integração com o front-end

O front-end publicado deve consumir a URL HTTPS final e enviar credenciais quando a autenticação depender do cookie, por exemplo com `credentials: 'include'` ou equivalente. A origem real do front-end deve ser permitida pela configuração CORS existente.

O Compose local permanece separado: usa HTTP em `localhost:8080` e não deve ser alterado para exigir ACME.

## 7. Validações e evidências

| Verificação | Resultado | Observação |
|---|---|---|
| `docker compose -f compose.production.yml config --quiet` | Passou | Compose de produção válido. |
| Validação do Caddyfile | Passou | Configuração reconhecida pela imagem Caddy validada. |
| Build/compilação | Passou | Imagem e aplicação compilam no fluxo atual. |
| `AuthenticationCookieIntegrationTest` | Passou | Cookie de produção com `Secure`/`HttpOnly` e chamada autenticada posterior. |
| `./mvnw clean verify` | 62 de 63 passaram | Uma falha preexistente em `ClinicStructureEndToEndIntegrationTest`: o teste espera `200`, enquanto o endpoint atual responde `201`. |
| `openspec validate adicionar-proxy-reverso-https-docker --strict` | Passou | Artefatos OpenSpec válidos. |
| Smoke test externo com DNS/firewall reais | Pendente | Não há, neste ambiente, domínio, credenciais ACME ou firewall público configurado para executar a prova ponta a ponta. |

A falha de `ClinicStructureEndToEndIntegrationTest` não foi alterada nesta change, pois não pertence ao proxy HTTPS e representa uma divergência preexistente entre teste e contrato atual do endpoint.

## 8. Atualizações posteriores à especificação inicial

### 8.1 Build Docker sem execução de testes

Após a implementação do proxy, o commit `a707826` (`20/11/2026 22:30`) alterou o estágio de build do [Dockerfile](../Dockerfile):

```dockerfile
RUN mvn -B clean package -DskipTests
```

Antes, o Dockerfile usava `mvn -B clean verify`. Isso reduz o acoplamento do build da imagem a testes de integração que dependem de infraestrutura externa. A validação completa dos testes continua sendo feita fora da construção da imagem, pelo Maven.

### 8.2 Cookie `SameSite` configurável

O commit `9a7710b` (`20/11/2026 23:56`) alterou o `AuthenticationController` para ler:

```java
@Value("${api.security.cookie.same-site:Lax}")
String jwtCookieSameSite;
```

O valor deixou de ser fixo em `Lax` e passou a ser aplicado a partir da configuração do ambiente.

### 8.3 Default de produção para `SameSite=None`

O commit `b425e8a` (`21/11/2026 01:20`) adicionou ao profile `prod`:

```yaml
same-site: ${JWT_COOKIE_SAME_SITE:None}
```

Isso permite autenticação por cookie quando o front-end e a API estiverem em origens diferentes, mantendo a possibilidade de ajuste por `JWT_COOKIE_SAME_SITE`. Como `SameSite=None` exige `Secure` nos navegadores modernos, a alteração depende da publicação HTTPS prevista nesta change.

### 8.4 Situação da documentação

Além deste registro, os guias [DEPLOY_PRODUCAO_DOCKER_UBUNTU.md](DEPLOY_PRODUCAO_DOCKER_UBUNTU.md), [GUIA_DEPLOY_E_INTEGRACAO_FRONTEND.md](GUIA_DEPLOY_E_INTEGRACAO_FRONTEND.md) e [IMPLEMENTAR_HTTPS_PROXY.md](IMPLEMENTAR_HTTPS_PROXY.md) foram alinhados ao Compose de produção, às variáveis de domínio/ACME, aos volumes do Caddy e à distinção entre execução local HTTP e publicação HTTPS.

## 9. Pendências e limites conhecidos

- A tarefa OpenSpec `4.2` permanece aberta até executar o smoke test com DNS e firewall reais.
- A change ainda não foi arquivada nem sincronizada com as specs principais; os artefatos da change continuam sendo a fonte de intenção desta implementação.
- O certificado real só será emitido quando `API_DOMAIN` resolver para o servidor e as portas `80`/`443` estiverem acessíveis externamente.
- O Compose não cria DNS, regras de firewall, conta ACME ou armazenamento externo de segredos.
- Não deve ser reaberta a publicação pública de `8080` ou `5432` como alternativa de diagnóstico; para diagnóstico, usar rede administrativa controlada ou acesso local.

## 10. Referências

- [Compose de produção](../compose.production.yml)
- [Caddyfile](../Caddyfile)
- [Dockerfile](../Dockerfile)
- [Configuração de produção do Spring](../src/main/resources/application-prod.yaml)
- [Teste de cookie de autenticação](../src/test/java/com/agendamento/smart/controller/auth/AuthenticationCookieIntegrationTest.java)
- [Guia de implementação HTTPS](IMPLEMENTAR_HTTPS_PROXY.md)
- [Deploy de produção Ubuntu/Docker](DEPLOY_PRODUCAO_DOCKER_UBUNTU.md)
- [Guia de integração com o front-end](GUIA_DEPLOY_E_INTEGRACAO_FRONTEND.md)

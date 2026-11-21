# HTTPS com proxy reverso

Esta configuração já está implementada no `compose.production.yml`. No host,
somente o proxy publica as portas `80` e `443`; a API e o PostgreSQL ficam
acessíveis apenas pelas redes Docker internas. O `docker-compose.yml` continua
reservado ao desenvolvimento/teste por HTTP.

O exemplo usa Caddy para terminar TLS e emitir/renovar certificados. O TLS
protege o tráfego entre clientes e Caddy. A conexão Caddy → API usa HTTP dentro
da rede Docker privada do projeto.

## Estado desejado

```text
Internet -- HTTP/80 e HTTPS/443 --> Caddy -- HTTP interno --> API:8080
                                                         API -- interno --> PostgreSQL:5432
```

Somente Caddy publica portas no host. A API participa das redes `edge` e
`backend`; o PostgreSQL participa apenas de `backend`. A API continua ouvindo
em `8080` e a aplicação continua conectando a `postgres:5432`.

## 1. Preparar DNS e acesso ao servidor

Escolha o domínio público da API e crie o registro DNS `A` apontando para o IP
público do servidor. Crie um registro `AAAA` somente se o servidor tiver IPv6
público corretamente configurado. O nome deve resolver para esse servidor antes
da primeira subida do proxy.

No firewall do provedor e no firewall do host, permita entrada TCP nas portas
`80` e `443`. Mantenha o acesso administrativo SSH restrito conforme a política
do servidor. Não crie regras de entrada para `5432` ou `8080`; portas publicadas
pelo Docker também devem ser conferidas, pois podem não seguir as regras do UFW
como esperado.

## 2. Usar o Compose de produção

Não altere o Compose local para publicar TLS. O arquivo `compose.production.yml`
já define `api`, `postgres` e `proxy`, usa a imagem fixada `caddy:2.9.1-alpine`,
publica somente 80/443 e mantém `api`/`postgres` sem `ports`. Os volumes
`postgresql_data`, `caddy_data` e `caddy_config` são nomeados e persistentes.

O arquivo exige `APP_VERSION`, `SPRING_PROFILES_ACTIVE`, credenciais do banco,
`JWT_SECRET`, `API_DOMAIN` e `ACME_EMAIL`. Mantenha esses valores em arquivo
fora do clone, com modo 600; nunca grave segredos no Compose ou no Caddyfile.

## 3. Configurar o Caddy

O `Caddyfile` versionado recebe o domínio e o e-mail por ambiente:

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

Com o nome de domínio resolvendo publicamente para o servidor e as portas `80`
e `443` alcançáveis, Caddy obtém e renova os certificados automaticamente e
redireciona HTTP para HTTPS. Os volumes `caddy_data` e `caddy_config` conservam
certificados e estado entre recriações do contêiner; não os remova durante
atualizações normais.

Não configure o proxy para encaminhar a API por um endereço de host como
`localhost:8080`. Dentro do contêiner, `localhost` identifica o próprio Caddy;
use o nome do serviço Docker `api:8080`.

## 4. Ajustar a aplicação para o proxy

Como TLS termina no Caddy, a aplicação recebe HTTP na rede Docker. Configure o
Spring Boot para interpretar os cabeçalhos encaminhados pelo proxy. Em
`src/main/resources/application-prod.yaml`, adicione no nível superior:

```yaml
server:
  forward-headers-strategy: framework
```

O proxy deve ser a única entrada pública da API e deve definir os cabeçalhos
`X-Forwarded-Proto`, `X-Forwarded-For` e `X-Forwarded-Host`. Caddy os define ao
encaminhar a requisição. Não publique a porta da API para que clientes externos
não possam contornar o proxy e enviar cabeçalhos encaminhados diretamente.

O cookie JWT recebe o atributo `Secure` conforme `JWT_COOKIE_SECURE`. O profile
`prod` usa `true` por padrão; mantenha também `JWT_COOKIE_SECURE=true` no
ambiente de produção. Em `test` e `homo`, o padrão é `false` para HTTP local.
HTTPS no proxy, por si só, não altera esse atributo do cookie. Confirme também
que o CORS de produção permite somente a origem real do front-end.

## 5. Subir e validar a mudança

Valide a configuração mesclada antes de aplicar:

```bash
docker compose --env-file /etc/agendamento-smart/agendamento-smart.prod.env \
  -f compose.production.yml config --quiet
docker compose --env-file /etc/agendamento-smart/agendamento-smart.prod.env \
  -f compose.production.yml config --services
```

Construa a API e suba os serviços. O proxy deve estar saudável e conseguir
alcançar a API pela rede `edge`:

```bash
docker compose --env-file /etc/agendamento-smart/agendamento-smart.prod.env \
  -f compose.production.yml up -d --build
docker compose --env-file /etc/agendamento-smart/agendamento-smart.prod.env \
  -f compose.production.yml ps
docker compose --env-file /etc/agendamento-smart/agendamento-smart.prod.env \
  -f compose.production.yml logs --tail=200 proxy
docker compose --env-file /etc/agendamento-smart/agendamento-smart.prod.env \
  -f compose.production.yml logs --tail=200 api
```

Verifique a publicação HTTP/HTTPS e o redirecionamento usando o domínio real:

```bash
curl -I "http://${API_DOMAIN}"
curl -I "https://${API_DOMAIN}"
```

A primeira chamada deve redirecionar para `https://`; a segunda deve completar
TLS com um certificado válido e alcançar a API. Use um endpoint de saúde não
sensível, caso exista, para verificar a resposta funcional sem consultar dados
de pacientes.

Por fim, teste de uma máquina externa ao servidor que as conexões diretas às
portas antigas não sejam aceitas:

```bash
nc -vz "${API_DOMAIN}" 8080
nc -vz "${API_DOMAIN}" 5432
```

Ambas devem falhar. Na saída de `docker compose ps`, somente `proxy` deve exibir
portas publicadas no host (`80` e `443`); `api` e `postgres` não devem exibir
mapeamentos. Verifique também as regras do firewall do provedor e do host.

## Critérios de conclusão

- [ ] DNS do domínio aponta para o servidor e o certificado HTTPS é válido.
- [ ] HTTP na porta `80` redireciona para HTTPS na porta `443`.
- [ ] O Compose publica no host somente `80` e `443`, pelo serviço `proxy`.
- [ ] API (`8080`) e PostgreSQL (`5432`) não possuem mapeamentos de porta.
- [ ] A API continua conectando ao banco por `postgres:5432` na rede Docker.
- [ ] Certificados e estado do Caddy persistem em volumes nomeados.
- [ ] O cookie JWT usa `Secure` em produção.
- [ ] Testes externos confirmam que `8080` e `5432` não são alcançáveis.

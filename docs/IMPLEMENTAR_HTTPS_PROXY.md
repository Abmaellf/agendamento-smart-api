# Implementar HTTPS com proxy reverso

Este documento descreve como alterar o deploy Docker para disponibilizar a API
por HTTPS atrás de um proxy reverso. No host, somente o proxy publica as portas
`80` e `443`; a API e o PostgreSQL ficam acessíveis apenas pela rede Docker.

O exemplo usa Caddy para terminar TLS e emitir/renovar certificados. O TLS
protege o tráfego entre clientes e Caddy. A conexão Caddy → API usa HTTP dentro
da rede Docker privada do projeto.

## Estado desejado

```text
Internet -- HTTP/80 e HTTPS/443 --> Caddy -- HTTP interno --> API:8080
                                                         API -- interno --> PostgreSQL:5432
```

Somente Caddy publica portas no host. A API precisa participar da rede do proxy
e da rede do banco; o PostgreSQL participa apenas da rede do banco. Remover as
publicações do Compose não altera as portas internas usadas pelos serviços:
a API continua ouvindo em `8080` e a aplicação continua conectando a
`postgres:5432`.

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

## 2. Alterar o `docker-compose.yml`

No serviço `api`, remova por completo a seção `ports` que usa
`AGENDA_API_BIND_ADDRESS` e `AGENDA_API_PORT`. Mantenha o serviço na rede
`backend` e adicione a rede `edge`:

```yaml
services:
  api:
    # Mantenha as demais opções atuais do serviço.
    networks:
      - backend
      - edge
```

No serviço `postgres`, remova por completo a seção `ports` que usa
`AGENDA_DB_BIND_ADDRESS` e `AGENDA_DB_PORT`. Mantenha somente a rede `backend`:

```yaml
  postgres:
    # Mantenha as demais opções atuais do serviço.
    networks:
      - backend
```

Adicione o serviço do proxy. Substitua `<versao-fixada>` por uma versão ou
digest do Caddy previamente homologado; não deixe uma tag flutuante em
produção.

```yaml
  proxy:
    image: caddy:<versao-fixada>
    restart: unless-stopped
    depends_on:
      - api
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy_data:/data
      - caddy_config:/config
    networks:
      - edge
    security_opt:
      - no-new-privileges:true
```

Declare as duas redes e os volumes persistentes do proxy junto às declarações
existentes no final do Compose. Preserve o volume PostgreSQL atual:

```yaml
networks:
  backend:
    driver: bridge
  edge:
    driver: bridge

volumes:
  postgresql_data:
  caddy_data:
  caddy_config:
```

O serviço `api` compartilha `edge` com o proxy, permitindo que Caddy encontre
`api:8080` pelo DNS interno do Compose. O PostgreSQL continua somente em
`backend`; o proxy não precisa se conectar ao banco. Não adicione `ports` à API
ou ao banco. A instrução `EXPOSE 8080` no Dockerfile é apenas metadado da imagem
e não publica a porta no host.

Depois de remover os mapeamentos, retire do `.env` de produção as variáveis
`AGENDA_API_BIND_ADDRESS`, `AGENDA_API_PORT`, `AGENDA_DB_BIND_ADDRESS` e
`AGENDA_DB_PORT`, pois não serão mais usadas pelo Compose.

## 3. Configurar o Caddy

Crie `Caddyfile` na raiz do repositório, ao lado do `docker-compose.yml`, e
troque o domínio de exemplo pelo domínio escolhido:

```caddyfile
api.exemplo.com {
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
docker compose config --quiet
docker compose config --services
```

Construa a API e suba os serviços. O proxy deve estar saudável e conseguir
alcançar a API pela rede `edge`:

```bash
docker compose up -d --build
docker compose ps
docker compose logs --tail=200 proxy
docker compose logs --tail=200 api
```

Verifique a publicação HTTP/HTTPS e o redirecionamento usando o domínio real:

```bash
curl -I http://api.exemplo.com
curl -I https://api.exemplo.com
```

A primeira chamada deve redirecionar para `https://`; a segunda deve completar
TLS com um certificado válido e alcançar a API. Use um endpoint de saúde não
sensível, caso exista, para verificar a resposta funcional sem consultar dados
de pacientes.

Por fim, teste de uma máquina externa ao servidor que as conexões diretas às
portas antigas não sejam aceitas:

```bash
nc -vz api.exemplo.com 8080
nc -vz api.exemplo.com 5432
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

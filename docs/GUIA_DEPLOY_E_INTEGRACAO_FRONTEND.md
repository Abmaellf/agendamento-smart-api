# Guia de deploy da API e integração com o front-end

Este documento descreve o procedimento baseado no código atual deste
repositório para publicar novas versões da API no Ubuntu e validar a
comunicação com o front-end em:

~~~text
https://agendamentos-smart.vercel.app/sign-in
~~~

O servidor informado possui atualmente a API em
http://62.238.8.131:8080, no contêiner agendamento-smart-api-1, e o PostgreSQL
no contêiner agendamento-smart-postgres-1.

## Conclusão da verificação do código atual

O `docker-compose.yml` continua sendo a fonte de verdade do ambiente local/teste.
Para produção pública, use `compose.production.yml`: os serviços são `api`,
`postgres` e `proxy`, a imagem da API é `agendamento-smart-api:<APP_VERSION>` e
os dados ficam nos volumes `postgresql_data`, `caddy_data` e `caddy_config`.

Há três condições importantes para o front funcionar em produção:

1. O front está em HTTPS, mas a URL atual da API é HTTP. O navegador bloqueia
   chamadas HTTPS para HTTP por mixed content. A URL pública da API deve ser
   publicada por HTTPS, por exemplo https://api.seu-dominio.com, através de um
   proxy reverso que encaminhe internamente para api:8080.
2. O login grava um cookie JWT HTTP-only e também devolve o token no corpo da
   resposta. O front deve usar credentials: "include" quando depender do
   cookie, ou enviar Authorization: Bearer <token> nas chamadas seguintes.
3. O CORS já inclui exatamente https://agendamentos-smart.vercel.app e
   allowCredentials=true. A origem adicional com /* é inválida como origem
   CORS e deve ser removida em uma correção futura; ela não invalida a origem
   exata que já está configurada.

Não foi possível acessar o IP ou o domínio Vercel a partir deste ambiente de
execução. Os smoke tests abaixo são, portanto, os procedimentos para confirmar
a integração no próprio servidor e no navegador, e não testes já executados.

## Contrato que o front deve usar

Defina uma única variável de ambiente no front, apontando para a URL HTTPS
final da API, sem barra final:

~~~dotenv
NEXT_PUBLIC_API_URL=https://api.seu-dominio.com
~~~

Enquanto a API continuar somente em HTTP, o front publicado em Vercel não
conseguirá consumi-la de forma confiável. O endereço
http://62.238.8.131:8080 pode ser usado apenas em teste local ou temporário,
sabendo que haverá bloqueio de conteúdo misto no navegador.

Rotas usadas pelo front e autenticação esperada:

| Uso | Método e rota | Autenticação | Resposta principal |
|---|---|---|---|
| Login | POST /auth/login | Pública | { token, user } e cookie jwt |
| Sessão atual | GET /auth/me | Cookie ou Bearer | Perfil com clinicId, tenantId e activeUnitId |
| Unidades | GET /api/units | ADMIN/USER | { content: [...] } |
| Pacientes | GET /api/patients | ADMIN/USER | { content: [...] } |
| Serviços | GET /api/services | ADMIN/USER | { content: [...] } |
| Profissionais | GET /api/professionals?serviceId=<uuid> | ADMIN/USER | { content: [...] } |
| Pacientes legados | GET /patient/list?page=0&size=5&name=... | ADMIN/USER | content, page, size, totalElements, totalPages, last |
| Criar paciente | POST /patient/save | ADMIN/USER | Paciente criado; unitIds é obrigatório |
| Criar agenda | POST /api/appointments | ADMIN/USER | Appointment com status AGENDADO |
| Listar agenda | GET /api/appointments?from=<instant>&to=<instant> | ADMIN/USER | { content: [...] } |
| Agenda por unidade | GET /api/appointments?unitId=<uuid>&from=<instant>&to=<instant> | ADMIN/USER | { content: [...] } |
| Alterar status | PATCH /api/appointments/<uuid>/status | ADMIN/USER | Appointment atualizado |
| Iniciar hora atual | POST /api/appointments/start-current-hour | ADMIN/USER | Resumo da operação |

Os instantes da agenda devem ser ISO-8601, preferencialmente em UTC, por
exemplo 2026-11-20T14:00:00Z. O intervalo de listagem é semiaberto:
[from,to).

Corpos mínimos:

~~~json
// POST /auth/login
{ "login": "usuario@example.com", "password": "senha-do-usuario" }
~~~

~~~json
// POST /patient/save
{
  "name": "Paciente de teste",
  "clinicId": null,
  "unitIds": ["<uuid-da-unidade>"]
}
~~~

~~~json
// POST /api/appointments
{
  "patientId": "<uuid-do-paciente>",
  "unitId": "<uuid-da-unidade>",
  "serviceId": "<uuid-do-servico>",
  "professionalId": null,
  "startsAt": "2026-11-20T14:00:00Z",
  "durationMinutes": 30,
  "price": 125.00
}
~~~

## Variáveis de ambiente corretas

Use um arquivo de produção fora do clone, por exemplo
/etc/agendamento-smart/agendamento-smart.prod.env, com modo 600. Não publique
esse arquivo no Git nem cole seus valores em logs.

~~~dotenv
SPRING_PROFILES_ACTIVE=prod

# Devem permanecer iguais aos dados do volume PostgreSQL já inicializado.
AGENDA_DB_NAME=agendamento-smart-api
AGENDA_DB_USER=<usuario-real-do-postgres>
AGENDA_DB_PASSWORD=<senha-real-do-postgres>

AGENDA_DB_BIND_ADDRESS=127.0.0.1
AGENDA_DB_PORT=5432

JWT_SECRET=<segredo-hexadecimal-longo>
JWT_COOKIE_SECURE=true

# Recomendado quando existir proxy HTTPS no mesmo servidor.
AGENDA_API_BIND_ADDRESS=127.0.0.1
AGENDA_API_PORT=8080

# Tag exclusiva para esta versão; não reutilize 0.0.1.
APP_VERSION=0.0.2
~~~

As variáveis têm estes efeitos:

- SPRING_PROFILES_ACTIVE=prod evita seeds de desenvolvimento e usa somente
  classpath:db/migration;
- AGENDA_DB_NAME, AGENDA_DB_USER e AGENDA_DB_PASSWORD configuram o PostgreSQL
  e a conexão interna da API;
- AGENDA_DB_BIND_ADDRESS e AGENDA_DB_PORT são o bind e a porta do host. A API
  usa internamente postgres:5432, não localhost:5432;
- JWT_SECRET é obrigatório para a inicialização e assina os tokens;
- JWT_COOKIE_SECURE=true só é compatível com publicação HTTPS;
- AGENDA_API_BIND_ADDRESS e AGENDA_API_PORT controlam a publicação da API;
- APP_VERSION forma a tag agendamento-smart-api:<versão>;
- AGENDA_APP_DIR e AGENDA_ENV_FILE são auxiliares do operador. Não são
  propriedades Spring e só têm efeito quando usados por
  docker compose --env-file ... ou por um wrapper;
- AGENDA_URL não precisa ser definida no Compose atual: ele a monta como
  jdbc:postgresql://postgres:5432/<AGENDA_DB_NAME>.

Gere segredos sem imprimir o arquivo final:

~~~bash
openssl rand -hex 32  # senha inicial do banco, se o banco ainda não existir
openssl rand -hex 64  # JWT_SECRET
~~~

Se o volume atual já tiver dados, não troque usuário, nome ou senha do banco
apenas no arquivo. O PostgreSQL não reaplica esses valores em um volume
existente. Uma troca de senha exige ALTER ROLE, atualização coordenada do
ambiente e uma janela de validação.

Se ainda não existir proxy HTTPS e for necessário manter o acesso temporário
por http://62.238.8.131:8080, o bind pode continuar em 0.0.0.0 e o cookie
seguro precisa estar desativado. Esse modo expõe HTTP sem proteção e não
resolve a comunicação do front HTTPS; use-o somente para diagnóstico e
substitua-o pelo cenário HTTPS antes do go-live.

## Publicação Docker com proxy HTTPS

Use um arquivo de ambiente fora do clone, com modo 600, contendo também o
domínio público e o e-mail usado pelo ACME:

~~~dotenv
SPRING_PROFILES_ACTIVE=prod
AGENDA_DB_NAME=agendamento-smart-api
AGENDA_DB_USER=<usuario-do-postgres>
AGENDA_DB_PASSWORD=<senha-do-postgres>
JWT_SECRET=<segredo-longo-e-exclusivo>
JWT_COOKIE_SECURE=true
APP_VERSION=<versao-ou-sha-imutavel>
API_DOMAIN=api.seu-dominio.com
ACME_EMAIL=ops@seu-dominio.com
~~~

O DNS de `API_DOMAIN` deve apontar para o servidor antes da primeira subida.
Libere somente TCP 80 e 443 no firewall. A API e o PostgreSQL não devem ter
portas publicadas no host.

Valide e suba a stack de produção:

~~~bash
export AGENDA_APP_DIR=/opt/agendamento-smart
export AGENDA_ENV_FILE=/etc/agendamento-smart/agendamento-smart.prod.env
cd "$AGENDA_APP_DIR"
docker compose --env-file "$AGENDA_ENV_FILE" \
  -f compose.production.yml config --quiet
docker compose --env-file "$AGENDA_ENV_FILE" \
  -f compose.production.yml up -d --build
docker compose --env-file "$AGENDA_ENV_FILE" \
  -f compose.production.yml ps
~~~

O `proxy` encaminha internamente para `api:8080`, termina TLS e persiste os
certificados nos volumes `caddy_data` e `caddy_config`. Não remova esses volumes
em atualizações normais e não use `docker compose down -v` em produção.

Teste o redirecionamento e a entrada HTTPS:

~~~bash
curl -I "http://${API_DOMAIN}"
curl -I "https://${API_DOMAIN}"
~~~

O `docker compose ps` deve mostrar portas somente no `proxy`. A partir de uma
máquina externa, 8080 e 5432 devem continuar inacessíveis.

## Preparação local antes de enviar uma feature

~~~bash
git status --short
./mvnw clean verify
docker compose config --quiet
~~~

Para validar a aplicação completa localmente, use um segredo de teste e o
profile test:

~~~bash
export JWT_SECRET='<segredo-local-longo-e-aleatorio>'
docker compose up -d --build --wait
docker compose ps
docker compose logs --tail=200 api
~~~

O profile test pode executar seeds de desenvolvimento. Nunca use esse profile no
servidor de produção.

Depois de revisar o diff, publique uma tag ou commit imutável:

~~~bash
git status --short
git add <arquivos-da-feature>
git commit -m "descreva a feature"
git push origin <branch>
git tag <versao-homologada>
git push origin <versao-homologada>
~~~

Não inclua .env, senhas, JWT_SECRET, dumps ou dados de pacientes no commit.

## Deploy no Ubuntu usando o Compose atual

Os comandos abaixo assumem que o clone está em /opt/agendamento-smart e o
ambiente está fora dele:

~~~bash
export AGENDA_APP_DIR=/opt/agendamento-smart
export AGENDA_ENV_FILE=/etc/agendamento-smart/agendamento-smart.prod.env
cd "$AGENDA_APP_DIR"
~~~

Confirme o estado antes de alterar contêineres:

~~~bash
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
docker compose --env-file "$AGENDA_ENV_FILE" ps
docker volume ls | grep agendamento-smart
git status --short
git rev-parse HEAD
~~~

O git status --short deve estar limpo. Faça backup antes de atualizar:

~~~bash
BACKUP_DIR=/var/backups/agendamento-smart
BACKUP_FILE="$BACKUP_DIR/agendamento-$(date -u +%Y%m%dT%H%M%SZ).dump"
install -d -m 700 "$BACKUP_DIR"
docker compose --env-file "$AGENDA_ENV_FILE" exec -T postgres sh -c \
  'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump --host=127.0.0.1 --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --format=custom --no-owner --no-acl' \
  > "$BACKUP_FILE"
chmod 600 "$BACKUP_FILE"
test -s "$BACKUP_FILE"
~~~

Atualize para uma tag ou commit homologado. Não use git pull cego se houver
mudanças locais:

~~~bash
git fetch --tags --prune
git checkout <tag-ou-commit-homologado>
git status --short
~~~

Valide o Compose sem revelar valores:

~~~bash
docker compose --env-file "$AGENDA_ENV_FILE" config --quiet
docker compose --env-file "$AGENDA_ENV_FILE" config --services
~~~

O segundo comando deve listar api e postgres.

Construa e substitua somente a API; o volume do banco não é removido:

~~~bash
docker compose --env-file "$AGENDA_ENV_FILE" build --pull api
docker compose --env-file "$AGENDA_ENV_FILE" up -d --no-deps --force-recreate api
docker compose --env-file "$AGENDA_ENV_FILE" ps
docker compose --env-file "$AGENDA_ENV_FILE" logs --tail=300 api
~~~

Esse procedimento causa uma breve indisponibilidade enquanto a API é
recriada. O Flyway executa migrations pendentes na inicialização.
docker compose down -v nunca deve ser usado em produção, pois remove o volume
do PostgreSQL.

Se for necessário recriar os dois serviços, preserve os dados com:

~~~bash
docker compose --env-file "$AGENDA_ENV_FILE" up -d --build --force-recreate
~~~

Não altere o nome do volume nem troque postgres por db: o arquivo atual usa o
serviço postgres, e os nomes observados são agendamento-smart-api-1 e
agendamento-smart-postgres-1.

## Validação no servidor

Como a API atual não possui endpoint público de healthcheck, valide por
camadas.

Contêineres e logs:

~~~bash
docker compose --env-file "$AGENDA_ENV_FILE" ps
docker compose --env-file "$AGENDA_ENV_FILE" logs --tail=200 postgres
docker compose --env-file "$AGENDA_ENV_FILE" logs --tail=300 api
~~~

Teste o preflight CORS contra a URL HTTPS do proxy, não contra o IP HTTP:

~~~bash
export API_BASE_URL=https://api.seu-dominio.com
curl -i -X OPTIONS "$API_BASE_URL/auth/login" \
  -H 'Origin: https://agendamentos-smart.vercel.app' \
  -H 'Access-Control-Request-Method: POST' \
  -H 'Access-Control-Request-Headers: content-type'
~~~

A resposta deve conter:

~~~text
Access-Control-Allow-Origin: https://agendamentos-smart.vercel.app
Access-Control-Allow-Credentials: true
~~~

Teste autenticação com uma conta autorizada, sem colocar a senha em histórico
de shell ou em scripts versionados:

~~~bash
read -r LOGIN
read -r -s PASSWORD; echo
curl -i -c /tmp/agendamento-smart.cookies \
  -H 'Content-Type: application/json' \
  -H 'Origin: https://agendamentos-smart.vercel.app' \
  --data "{\"login\":\"$LOGIN\",\"password\":\"$PASSWORD\"}" \
  "$API_BASE_URL/auth/login"
curl -i -b /tmp/agendamento-smart.cookies \
  -H 'Origin: https://agendamentos-smart.vercel.app' \
  "$API_BASE_URL/auth/me"
rm -f /tmp/agendamento-smart.cookies
~~~

O login deve responder 200, criar o cookie jwt e devolver token e user.
`/auth/me` deve responder 200 com clinicId, tenantId e activeUnitId.

Se o front usar cookie:

~~~javascript
fetch(API_BASE_URL + "/auth/me", { credentials: "include" });
~~~

Se usar o token do corpo do login:

~~~javascript
fetch(API_BASE_URL + "/api/units", {
  headers: { Authorization: "Bearer " + token }
});
~~~

No navegador, em DevTools > Network na tela /sign-in, confirme que a
requisição vai para a URL HTTPS da API, que o preflight não falha, que o login
retorna 200, que as chamadas seguintes levam cookie ou Bearer e que /auth/me
retorna a clínica e a unidade esperadas.

## Atualizações futuras e rollback

Use uma nova versão de imagem em cada deploy:

~~~bash
sed -i 's/^APP_VERSION=.*/APP_VERSION=0.0.3/' "$AGENDA_ENV_FILE"
docker compose --env-file "$AGENDA_ENV_FILE" build --pull api
docker compose --env-file "$AGENDA_ENV_FILE" up -d --no-deps --force-recreate api
docker compose --env-file "$AGENDA_ENV_FILE" logs --tail=300 api
~~~

Antes de liberar, registre commit, tag da imagem, backup e resultado dos
testes. Rollback de imagem não desfaz migrations de banco. Se a migration nova
não for compatível com a imagem anterior, será necessário restaurar o backup
ou usar uma estratégia expand/contract; não execute flyway repair para esconder
divergências.

## Pendências que impedem considerar o ambiente plenamente pronto

- manter o proxy reverso HTTPS ativo e deixar API/PostgreSQL acessíveis somente
  na rede interna;
- remover a origem CORS inválida
  https://agendamentos-smart.vercel.app/*;
- confirmar se a estratégia final será cookie cross-site ou Bearer. O cookie
  atual usa SameSite=Lax, e cookies cross-site normalmente exigem política
  compatível com HTTPS e SameSite=None;
- revisar LoginResponseDTO, que retorna a entidade User diretamente, e garantir
  que nenhum segredo ou hash seja exposto na resposta;
- criar um healthcheck HTTP dedicado, sem depender de rota de negócio protegida;
- confirmar o bootstrap da primeira clínica, unidade, usuário e catálogo no
  profile prod, que não carrega seeds;
- manter postgres:18 e imagens-base do Docker fixados em versão/digest
  homologado.

Enquanto HTTPS, autenticação e bootstrap não estiverem validados, o fato de o
contêiner estar Up não significa que o front conseguirá fazer login ou usar a
agenda.


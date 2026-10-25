# Deploy de produção no Ubuntu com Docker

Este roteiro parte de um servidor Ubuntu no qual Git, Docker e o plugin Docker
Compose já estão instalados, e o repositório foi clonado em
`$HOME/application`. A API será compilada em Docker e tanto ela quanto o
PostgreSQL serão executados exclusivamente em contêineres. O Java 21 instalado
no host não participa do deploy; ele pode continuar instalado apenas para
testes manuais.

O `Dockerfile` já usa JDK 21 e Maven durante o build e JRE 21 no contêiner
final. O Compose inicia a API e o PostgreSQL, persiste o banco no volume
`agendamento-smart_postgresql_data` e executa as migrations Flyway quando a
API sobe.

## Antes de expor o serviço na internet

O profile `prod` configura a aplicação para produção, mas não corrige por si
só todos os requisitos de segurança. Na versão atual do repositório, conclua
estes itens antes do *go-live* público:

- remova o `.env` do versionamento, pois ele ainda está rastreado pelo Git,
  e rotacione todo segredo que já tenha sido commitado;
- substitua `bitnami/postgresql:latest` por uma tag imutável ou digest já
  homologado; a tag `latest` pode trocar inclusive a versão principal do banco;
- disponibilize a API somente por HTTPS, atrás de um proxy reverso; publique
  apenas `80` e `443` e não exponha `5432` ou `8080` diretamente;
- configure `JWT_COOKIE_SECURE=true` para emitir o cookie JWT com `Secure` em
  produção e sirva a API por HTTPS;
- impeça que a entidade `User` serialize o campo `password` e revise os
  endpoints que retornam a entidade diretamente;
- corrija o isolamento por clínica: `GET /patient/list` é público e consulta
  pacientes sem a fronteira do tenant;
- restrinja a criação de usuários a administradores e não aceite do cliente o
  papel ou a clínica sem validação de autorização; hoje
  `POST /user/register/{clinicId}` não exige `ADMIN` e recebe esses dados do
  cliente;
- revise a combinação de autenticação por cookie com CSRF desabilitado;
- externalize e restrinja o CORS aos domínios reais de produção;
- defina um processo auditado para criar a primeira clínica, unidade e conta
  administrativa; o profile `prod` não carrega seeds e o projeto ainda não
  oferece esse fluxo de bootstrap;
- defina limites de CPU/memória e dimensionamento de heap compatíveis com o
  servidor, além de alertas de disco para o banco e os backups;
- adicione um healthcheck HTTP da API. Atualmente `docker compose up --wait`
  comprova a saúde do banco, mas apenas verifica se o contêiner da API continua
  em execução.

Enquanto esses itens não forem concluídos, é possível validar o deploy de
produção de forma privada, mantendo a API ligada a `127.0.0.1`.

## 1. Corrigir o `.env` rastreado

Faça esta correção no repositório e publique o commit **antes** de inserir
segredos de produção no servidor:

```bash
git ls-files --error-unmatch .env
git rm --cached .env
git commit -m "remove .env do versionamento"
git push
```

O `.gitignore` já possui a regra para `.env`, mas ela não remove arquivos que
foram rastreados anteriormente. Remover o arquivo do commit atual também não
apaga cópias do histórico: considere todo valor anteriormente commitado como
comprometido e rotacione-o. Não reescreva o histórico de um repositório
compartilhado sem coordenar a operação com a equipe.

No servidor, atualize o clone somente depois que essa correção estiver no
remoto:

```bash
cd "$HOME/application"
git pull --ff-only
if git ls-files --error-unmatch .env >/dev/null 2>&1; then
  echo "ERRO: .env ainda está versionado" >&2
  false
else
  echo "OK: .env não está versionado"
fi
```

Não prossiga se o teste indicar que `.env` ainda está versionado.

## 2. Registrar as variáveis do host no `~/.bashrc`

Edite o arquivo do usuário Linux dedicado ao deploy:

```bash
nano ~/.bashrc
```

Adicione ao final:

```bash
# Agendamento Smart
export AGENDA_APP_DIR="$HOME/application"
export AGENDA_ENV_FILE="$HOME/.config/agendamento-smart/.env"

# Executa sempre o Compose do projeto e evita que valores antigos exportados
# no shell sobrescrevam silenciosamente o arquivo de produção.
agenda-compose() {
  env \
    -u SPRING_PROFILES_ACTIVE \
    -u AGENDA_DB_NAME \
    -u AGENDA_DB_USER \
    -u AGENDA_DB_PASSWORD \
    -u AGENDA_DB_BIND_ADDRESS \
    -u AGENDA_DB_PORT \
    -u AGENDA_URL \
    -u AGENDA_FLYWAY_LOCATIONS \
    -u JWT_SECRET \
    -u JWT_COOKIE_SECURE \
    -u AGENDA_API_BIND_ADDRESS \
    -u AGENDA_API_PORT \
    -u APP_VERSION \
    docker compose \
      --project-directory "$AGENDA_APP_DIR" \
      --file "$AGENDA_APP_DIR/docker-compose.yml" \
      --env-file "$AGENDA_ENV_FILE" \
      "$@"
}
```

Depois, recarregue a sessão:

```bash
source ~/.bashrc
cd "$AGENDA_APP_DIR"
```

As variáveis do Ubuntu ficam no `~/.bashrc`, mas nele há somente caminhos e a
função operacional. Senha do banco e segredo JWT permanecem no arquivo
protegido, fora do clone, e não são exportados globalmente para todos os
subprocessos do shell.

Use `agenda-compose` nos comandos interativos deste guia. A função remove do
ambiente as variáveis que poderiam prevalecer sobre `--env-file`, seleciona o
Compose correto e carrega sempre o arquivo de produção. Remova do `~/.bashrc`
quaisquer `export` antigos dessas variáveis para não confundir outras
ferramentas.

O `~/.bashrc` não é carregado de forma confiável por cron, systemd ou shells não
interativos. Uma automação futura deve usar caminhos absolutos, um script
protegido e `docker compose --env-file "$AGENDA_ENV_FILE"`; ela não deve depender
apenas do `~/.bashrc`.

## 3. Validar Docker e o código que será implantado

```bash
docker --version
docker compose version
command -v openssl curl nano >/dev/null || {
  echo "ERRO: instale openssl, curl e nano antes de continuar." >&2
  false
}
sudo systemctl enable --now docker
docker info >/dev/null
cd "$AGENDA_APP_DIR"
git status --short
git log -1 --oneline
```

O `git status --short` deve ficar vazio. Em produção, prefira uma tag de release
ou um commit previamente homologado, em vez de implantar uma branch em
movimento:

```bash
git fetch --tags --prune
git checkout <tag-ou-commit-homologado>
```

Se `docker info` falhar por permissão, configure conscientemente o usuário no
grupo `docker` e abra uma nova sessão. A associação a esse grupo equivale, na
prática, a conceder privilégios administrativos no host. Evite executar o
Compose com `sudo`, pois o `sudo` pode descartar as variáveis e criar arquivos
pertencentes ao `root`.

Antes do primeiro deploy, substitua no `docker-compose.yml`:

```yaml
image: bitnami/postgresql:latest
```

por uma tag patch ou digest imutável que tenha sido testado com um banco
descartável. Faça essa alteração no repositório, passe pela homologação e
publique-a em commit; não mantenha uma edição exclusiva e não rastreada no
servidor. O mesmo princípio pode ser aplicado às imagens-base do `Dockerfile`.

Se o firewall UFW for usado, permita SSH antes de ativá-lo e publique somente
as portas do proxy HTTPS:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw status
```

Não abra `5432` ou `8080` para a internet. Portas publicadas pelo Docker podem
contornar regras do UFW; use bind em loopback e, no desenho com proxy Docker,
remova a publicação das portas da API e do banco.

## 4. Criar o `.env` de produção

Mantenha o arquivo fora do clone para que uma troca de tag não o sobrescreva e
para que ele não seja confundido com o `.env` histórico do repositório:

```bash
install -d -m 700 "$(dirname "$AGENDA_ENV_FILE")"
if [ -e "$AGENDA_ENV_FILE" ]; then
  echo "ERRO: o arquivo de ambiente já existe; não será sobrescrito." >&2
else
  umask 077
  install -m 600 "$AGENDA_APP_DIR/.env.example" "$AGENDA_ENV_FILE"
fi
nano "$AGENDA_ENV_FILE"
```

Use este modelo no arquivo apontado por `AGENDA_ENV_FILE`:

```dotenv
SPRING_PROFILES_ACTIVE=prod

AGENDA_DB_NAME=agendamento_smart
AGENDA_DB_USER=agenda_prod
AGENDA_DB_PASSWORD=<senha-hexadecimal-aleatoria>
AGENDA_DB_BIND_ADDRESS=127.0.0.1
AGENDA_DB_PORT=5432

JWT_SECRET=<segredo-hexadecimal-aleatorio-e-exclusivo>
JWT_COOKIE_SECURE=true
AGENDA_API_BIND_ADDRESS=127.0.0.1
AGENDA_API_PORT=8080
APP_VERSION=<versao-ou-sha-imutavel>
```

Gere valores independentes sem reutilizar segredos de teste ou homologação:

```bash
openssl rand -hex 32
openssl rand -hex 64
git rev-parse --short=12 HEAD
```

Use a primeira saída como senha do banco, a segunda como `JWT_SECRET` e a
terceira, opcionalmente combinada com a versão da release, como `APP_VERSION`.
Cole apenas os valores no arquivo, sem os sinais `<` e `>` do modelo.

Observações importantes:

- `SPRING_PROFILES_ACTIVE=prod` é obrigatório. O fallback atual do Compose é
  `test`, que também carrega seeds de desenvolvimento;
- `AGENDA_URL` não deve ser adicionada. O Compose a monta internamente usando
  `db:5432`, que é o endereço correto na rede dos contêineres;
- `AGENDA_FLYWAY_LOCATIONS` não precisa ser definida. O profile `prod` já usa
  somente `classpath:db/migration`, sem seeds, e o Compose atual não repassa
  essa variável ao contêiner;
- mantenha os binds em `127.0.0.1`. O banco não deve ser público e a API deve
  permanecer privada até existir um proxy HTTPS;
- mudar `AGENDA_DB_NAME`, `AGENDA_DB_USER` ou `AGENDA_DB_PASSWORD` depois que o
  volume foi inicializado não migra automaticamente o banco existente.

Proteja e confira o arquivo sem imprimir os segredos:

```bash
chmod 600 "$AGENDA_ENV_FILE"
test -r "$AGENDA_ENV_FILE"

(
  set -a
  . "$AGENDA_ENV_FILE"
  set +a

  test "${SPRING_PROFILES_ACTIVE:-}" = "prod" \
    && test "${AGENDA_DB_NAME:-}" != "" \
    && test "${AGENDA_DB_USER:-}" != "" \
    && test "${AGENDA_DB_PASSWORD:-}" != "" \
    && test "${JWT_SECRET:-}" != "" \
    && test "${JWT_COOKIE_SECURE:-}" = "true" \
    && test "${APP_VERSION:-}" != "" \
    && test "${AGENDA_API_BIND_ADDRESS:-}" = "127.0.0.1" \
    && test "${AGENDA_DB_BIND_ADDRESS:-}" = "127.0.0.1" \
    && test "${AGENDA_DB_PASSWORD:-}" != "agenda-local-password" \
    && test "${APP_VERSION:-}" != "local" \
    && test "${#AGENDA_DB_PASSWORD}" -ge 32 \
    && test "${#JWT_SECRET}" -ge 64 \
    && case "$AGENDA_DB_PASSWORD" in *[!0-9a-fA-F]*) false;; esac \
    && case "$JWT_SECRET" in *[!0-9a-fA-F]*) false;; esac \
    && case "$AGENDA_DB_NAME$AGENDA_DB_USER$APP_VERSION" in *'<'*|*'>'*) false;; esac
) \
  && echo "Variáveis essenciais carregadas." \
  || { echo "ERRO: configuração de produção incompleta." >&2; false; }
```

O modo `600` protege o arquivo no host, mas root e qualquer pessoa com acesso ao
socket/grupo Docker ainda pode obter variáveis com `docker inspect`. Nunca
compartilhe saídas completas de `docker inspect` ou `docker compose config`; em
ambientes mais críticos, prefira um gerenciador de segredos compatível com o
orquestrador.

## 5. Validar a configuração sem iniciar contêineres

```bash
cd "$AGENDA_APP_DIR"
agenda-compose config --quiet
agenda-compose config --services
```

O segundo comando deve listar `api` e `db`. O primeiro não deve produzir erro.
Não use valores default em produção, mesmo que o Compose permita.

## 6. Fazer o primeiro deploy

O build ocorre dentro do Docker. Ele baixa as dependências com Maven, executa
`mvn clean verify` usando JDK 21 e cria uma imagem final com JRE 21. Não execute
`java`, `mvn` ou `./mvnw` no host para este deploy.
Como o repositório ainda não possui testes em `src/test`, essa etapa comprova
build e empacotamento, mas não substitui testes de regressão ou homologação.

```bash
cd "$AGENDA_APP_DIR"
agenda-compose pull db
agenda-compose build --pull api
agenda-compose up -d --wait
agenda-compose ps
```

Na primeira inicialização, o Flyway cria o schema no volume persistente. Não
interrompa o processo durante as migrations.

## 7. Confirmar a inicialização

Primeiro, examine os logs sem mantê-los abertos indefinidamente:

```bash
agenda-compose logs --tail=200 db
agenda-compose logs --tail=300 api
```

Não deve haver erro de profile, autenticação no PostgreSQL, Flyway, validação
do Hibernate ou resolução de `JWT_SECRET`.

Consulte o histórico do Flyway diretamente no banco:

```bash
agenda-compose exec -T db sh -c \
  'PGPASSWORD="$POSTGRESQL_PASSWORD" psql --host=127.0.0.1 --username="$POSTGRESQL_USERNAME" --dbname="$POSTGRESQL_DATABASE" --command="SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;"'
```

Todas as linhas devem ter `success` igual a `t`. Por fim, faça um teste HTTP
local que também acessa o banco:

```bash
curl --fail --silent --show-error \
  "http://127.0.0.1:8080/patient/list" >/dev/null \
  && echo "API e banco responderam."
```

Esse teste compensa a ausência atual de healthcheck HTTP no serviço `api`.
Quando `/patient/list` for corretamente protegido, substitua esse smoke test
por um endpoint de saúde dedicado que não exponha dados de negócio.

## 8. Planejar o bootstrap inicial

No profile `prod`, o Flyway cria somente o schema: as seeds de desenvolvimento
não são executadas. Isso é intencional, mas o projeto atual ainda não fornece
um fluxo de produção para criar a primeira clínica, sua unidade padrão e o
primeiro administrador. A API pode estar tecnicamente saudável e ainda assim
não estar utilizável.

Antes do *go-live*, implemente e homologue um bootstrap administrativo
controlado, idempotente e auditável. Ele deve gerar a senha pelo mesmo encoder
BCrypt da aplicação, limitar quem pode executá-lo e ser desativado ou tornar-se
inofensivo depois do primeiro uso. Registre quem executou, quando e quais
identificadores foram criados.

Não resolva esse problema com `SPRING_PROFILES_ACTIVE=test`, não inclua
`classpath:db/seed/dev` no Flyway de produção e não copie as seeds existentes:
elas foram feitas para bancos descartáveis e podem conter credenciais e dados
conhecidos. Também não insira senha em texto puro diretamente no PostgreSQL.

## 9. Reinício automático e operação diária

Os serviços usam `restart: unless-stopped`. Com o daemon Docker habilitado, os
contêineres voltam após o reboot do servidor, exceto quando foram parados
manualmente.

Comandos seguros para a rotina:

```bash
cd "$AGENDA_APP_DIR"
agenda-compose ps
agenda-compose logs --tail=200 api
agenda-compose logs --since=30m api
docker stats --no-stream
docker system df
```

Os logs dos dois serviços já usam rotação local de três arquivos de 10 MB.
Para acompanhar temporariamente:

```bash
agenda-compose logs --follow --tail=100 api
```

Encerre o acompanhamento com `Ctrl+C`; isso não para o contêiner.

Para parar e iniciar sem apagar dados:

```bash
agenda-compose stop
agenda-compose start
```

`docker compose down` remove contêineres e a rede, mas preserva o volume se for
executado sem `-v`. **Nunca use `docker compose down -v` em produção**: a opção
`-v` remove o volume que contém o PostgreSQL.

## 10. Fazer backup do PostgreSQL

Faça backup antes de toda atualização e também em agenda automática. O comando
abaixo cria um dump lógico no host, fora do volume Docker:

```bash
cd "$AGENDA_APP_DIR"
BACKUP_DIR="$HOME/backups/agendamento-smart"
BACKUP_FILE="$BACKUP_DIR/agendamento-$(date -u +%Y%m%dT%H%M%SZ).dump"
BACKUP_TMP="${BACKUP_FILE}.tmp"

install -d -m 700 "$BACKUP_DIR"
if agenda-compose exec -T db sh -c \
  'PGPASSWORD="$POSTGRESQL_PASSWORD" pg_dump --host=127.0.0.1 --username="$POSTGRESQL_USERNAME" --dbname="$POSTGRESQL_DATABASE" --format=custom --no-owner --no-acl' \
  > "$BACKUP_TMP" \
  && test -s "$BACKUP_TMP" \
  && agenda-compose exec -T db pg_restore --list < "$BACKUP_TMP" >/dev/null; then
  chmod 600 "$BACKUP_TMP"
  mv "$BACKUP_TMP" "$BACKUP_FILE"
  echo "Backup criado e estrutura validada: $BACKUP_FILE"
else
  rm -f "$BACKUP_TMP"
  echo "ERRO: backup inválido." >&2
  false
fi
```

Um arquivo no mesmo servidor não é uma estratégia completa. Copie os dumps
para armazenamento externo protegido, defina retenção e teste periodicamente a
restauração em um banco isolado. `pg_restore --list` valida o arquivo, mas não
substitui um teste real de restauração. Não teste restauração sobre a base de
produção.

## 11. Atualizar a aplicação

Use somente uma release homologada e mantenha a imagem anterior até concluir o
teste da nova versão:

1. Gere e copie para fora do servidor um backup válido.
2. Registre o commit e o `APP_VERSION` atuais.
3. Confirme que o repositório está limpo.
4. Busque e selecione a nova tag ou commit.
5. Atualize `APP_VERSION` no arquivo apontado por `AGENDA_ENV_FILE` para uma
   tag nova e específica (ou, preferencialmente, use o digest da imagem).
6. Valide, construa, recrie somente a API e execute novamente os testes da
   seção anterior.

```bash
cd "$AGENDA_APP_DIR"
git status --short
git rev-parse HEAD
git fetch --tags --prune
git checkout <nova-tag-ou-commit-homologado>
if git ls-files --error-unmatch .env >/dev/null 2>&1; then
  echo "ERRO: a release selecionada ainda rastreia .env." >&2
  false
fi
nano "$AGENDA_ENV_FILE"

agenda-compose config --quiet
agenda-compose build --pull api
agenda-compose up -d --no-deps api
agenda-compose ps
agenda-compose logs --tail=300 api
```

Antes do build, registre `git rev-parse HEAD`, `agenda-compose config --images` e
o ID da imagem anterior. O Flyway pode alterar o schema durante a subida. Um
rollback da imagem não desfaz migrations; não existe *down migration* automática.
Volte à imagem anterior apenas se o schema novo for compatível. Caso contrário,
uma restauração do dump pré-deploy substitui a base inteira e perde gravações
posteriores ao backup, exigindo janela de manutenção ou uma estratégia
expand/contract. Nunca execute `flyway repair` apenas para fazer a aplicação
iniciar.

`APP_VERSION` deve mudar a cada release. Se a mesma tag for reutilizada, o
`build` sobrescreve a imagem anterior e elimina o rollback rápido por imagem.

Para reutilizar uma imagem anterior que ainda existe no host, selecione também
o código/Compose correspondente, restaure o `APP_VERSION` anterior no arquivo
apontado por `AGENDA_ENV_FILE` e confirme a imagem antes de recriar somente a
API:

```bash
agenda-compose config --images | grep -Eq '^agendamento-smart-api:'
agenda-compose up -d --no-build --no-deps api
```

Depois do rollback, repita a inspeção dos logs e o teste HTTP. Não apague a
imagem anterior nem o backup durante a janela de validação.

## 12. Publicação HTTPS

O repositório ainda não inclui proxy reverso nem certificados TLS. Para manter
toda a aplicação em Docker, adicione ao Compose um proxy como Caddy, Nginx ou
Traefik, ligado à mesma rede da API. O proxy deve:

- publicar somente `80:80` e `443:443`;
- encaminhar internamente para `api:8080`;
- armazenar certificados em volume persistente;
- redirecionar HTTP para HTTPS;
- definir limites de requisição e cabeçalhos de proxy apropriados;
- possuir tag/digest fixo e rotação de logs.

Nesse desenho, remova a publicação de porta do serviço `api` e a publicação
de porta do serviço `db`; ambos ficam acessíveis apenas pela rede Docker. Ajuste
DNS, CORS e o cookie seguro para o domínio real antes de liberar o tráfego. Não
troque simplesmente o bind da API para `0.0.0.0`: isso publicaria HTTP sem TLS e
contornaria a camada de entrada planejada.

O repositório ainda não entrega um `compose.production.yml`, um `Caddyfile` ou
outro artefato de proxy. Portanto, seguindo apenas os arquivos atuais, o
resultado verificável é um deploy privado em loopback; a publicação pública só
fica concluída depois que esse proxy Docker for adicionado, testado com o DNS e
validado com um endpoint de saúde HTTP real.

## Checklist final

- [ ] O arquivo apontado por `AGENDA_ENV_FILE` está fora do clone e possui modo
      `600`; o `.env` histórico não está rastreado.
- [ ] Segredos anteriormente versionados foram rotacionados.
- [ ] `SPRING_PROFILES_ACTIVE` é exatamente `prod`.
- [ ] Senha do banco e `JWT_SECRET` são fortes, exclusivos e não usam defaults.
- [ ] PostgreSQL e imagens-base estão fixados em versões homologadas.
- [ ] Banco e API não estão diretamente expostos à internet.
- [ ] Pendências de cookie, serialização, endpoints públicos, CSRF e CORS foram
      corrigidas e revisadas.
- [ ] Isolamento por clínica, autorização de papéis e bootstrap inicial foram
      implementados e homologados.
- [ ] Proxy HTTPS e healthcheck da API estão ativos.
- [ ] Migrations terminaram com sucesso e o smoke test HTTP passou.
- [ ] Backup externo existe e a restauração foi testada.
- [ ] Commit implantado, `APP_VERSION` e procedimento de rollback foram
      registrados.

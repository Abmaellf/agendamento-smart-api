# Profiles e build da API

A aplicação possui três profiles Spring de execução. O `pom.xml` também oferece profiles Maven com os mesmos nomes como atalhos exclusivos para `spring-boot:run`:

| Profile | Finalidade | Flyway | Logs SQL |
|---|---|---|---|
| `test` | Testes locais e ambiente integrado descartável | Migrations e seeds de desenvolvimento por padrão | SQL em `DEBUG`, sem valores dos parâmetros |
| `homo` | Homologação antes da promoção | Somente migrations por padrão | Desabilitados |
| `prod` | Produção | Somente migrations por padrão | Desabilitados |

As propriedades comuns ficam em `application.yaml`; cada `application-<profile>.yaml` contém apenas as diferenças do ambiente. Credenciais e o segredo JWT não ficam nesses arquivos.

Os profiles Maven não geram três builds. `./mvnw clean verify` continua produzindo um único JAR com os três YAMLs, e o ambiente é selecionado somente ao iniciar a aplicação.

## Variáveis de ambiente

Defina valores próprios em cada ambiente:

| Variável | Obrigatória | Uso |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Condicional | Seleciona `test`, `homo` ou `prod` quando o profile não vier do Maven/IntelliJ |
| `AGENDA_URL` | Sim | URL JDBC do PostgreSQL |
| `AGENDA_DB_USER` | Sim | Usuário do banco |
| `AGENDA_DB_PASSWORD` | Sim | Senha do banco |
| `JWT_SECRET` | Sim | Segredo exclusivo e forte para assinar os tokens |
| `JWT_COOKIE_SECURE` | Não | Atributo `Secure` do cookie JWT; padrão `false` em `test`/`homo` e `true` em `prod`. No Compose, defina `true` explicitamente em produção |
| `AGENDA_FLYWAY_LOCATIONS` | Não | Sobrescreve os locais de migrations/seeds |

Não versione esses valores. Em homologação e produção, injete-os pelo serviço de deploy ou por um gerenciador de segredos.

Para um novo ambiente local, copie `.env.example` para `.env`, preencha `AGENDA_DB_PASSWORD` e `JWT_SECRET` e mantenha o `.env` somente na máquina do desenvolvedor:

```bash
cp .env.example .env
```

O Docker Compose carrega esse `.env` automaticamente para interpolar o arquivo. Maven e IntelliJ não o carregam por conta própria; neles, configure as variáveis na sessão do terminal ou na Run Configuration.

Este repositório já possuía um `.env` rastreado antes da inclusão da regra no `.gitignore`. A regra não deixa de rastrear um arquivo que já está no histórico. Após confirmar que os valores locais estão guardados, remova-o apenas do índice em uma alteração própria e rotacione qualquer segredo real que já tenha sido versionado:

```bash
git rm --cached .env
```

O `.dockerignore` também exclui o `.env` do contexto de build. Para `homo` e `prod`, não envie esse arquivo junto à imagem; use o mecanismo de segredos da plataforma.

## Passo a passo

### 1. Confira os pré-requisitos

Use Java 21 e o Maven Wrapper do projeto:

```bash
java -version
./mvnw -version
```

O banco PostgreSQL do ambiente deve existir e estar acessível. O Flyway cria e atualiza o schema quando a aplicação inicia.

### 2. Configure o ambiente

Exemplo para uma base local de teste:

```bash
export AGENDA_URL='jdbc:postgresql://localhost:5432/agendamento-smart-api'
export AGENDA_DB_USER='<usuario-do-banco>'
export AGENDA_DB_PASSWORD='<senha-do-banco>'
export JWT_SECRET='<segredo-longo-e-aleatorio>'
```

Troque conexão, usuário, senha e segredo antes de iniciar `homo` ou `prod`. Nunca reutilize o segredo de um ambiente em outro.

No profile `prod`, o cookie JWT usa `Secure` por padrão. Defina
`JWT_COOKIE_SECURE=true` explicitamente no ambiente de produção; em `test` e
`homo`, o padrão é `false` para permitir execução local por HTTP.

O profile `test` inclui os dados de demonstração por padrão. Para executar apenas as migrations nesse profile:

```bash
export AGENDA_FLYWAY_LOCATIONS='classpath:db/migration'
```

### 3. Valide e gere o build

Na raiz do projeto, execute:

```bash
./mvnw clean verify
```

Esse comando compila, executa os testes e empacota o JAR em `target/`. Atualmente o projeto ainda não possui testes automatizados, portanto a validação completa exige também iniciar a aplicação contra um PostgreSQL válido.

O build é único: o mesmo JAR deve ser promovido de `test` para `homo` e depois para `prod`. Não é necessário nem recomendado usar `-Ptest`, `-Phomo` ou `-Pprod` no build.

### 4. Execute pelo Maven

Os profiles Maven configuram a propriedade oficial `spring-boot.run.profiles`:

```bash
./mvnw -Ptest spring-boot:run
./mvnw -Phomo spring-boot:run
./mvnw -Pprod spring-boot:run
```

Execute somente uma das linhas. Não ative dois profiles Maven ao mesmo tempo e não combine `-Ptest` com outro valor em `SPRING_PROFILES_ACTIVE`; o argumento gerado pelo plugin pode prevalecer e esconder uma configuração incorreta.

Com o JAR já gerado:

```bash
SPRING_PROFILES_ACTIVE=test java -jar target/agendamento-smart-api-*.jar
SPRING_PROFILES_ACTIVE=homo java -jar target/agendamento-smart-api-*.jar
SPRING_PROFILES_ACTIVE=prod java -jar target/agendamento-smart-api-*.jar
```

Execute somente uma dessas linhas e forneça antes as variáveis correspondentes ao ambiente. No log de inicialização, confirme o profile ativo e o sucesso das migrations do Flyway e da validação do Hibernate.

O profile Maven `test` não ativa automaticamente o profile Spring nos testes JUnit. Testes futuros devem usar `@ActiveProfiles("test")` ou configuração equivalente.

### 5. Execute pelo IntelliJ IDEA

#### Usando os profiles Maven adicionados ao `pom.xml`

1. Inicie somente o PostgreSQL conforme a próxima seção.
2. No painel **Maven**, clique em **Reload All Maven Projects**.
3. Acesse **Run > Edit Configurations**, clique em **+** e selecione **Maven**.
4. Defina o nome `API - test (Maven)`.
5. Use a raiz do projeto em **Working directory**, selecione **JRE 21** e informe em **Run**:

```text
-Ptest spring-boot:run
```

6. Em **Environment variables**, informe os valores efetivamente usados pelo serviço `db` do Compose:

```text
AGENDA_URL=jdbc:postgresql://localhost:5432/<nome-do-banco>
AGENDA_DB_USER=<mesmo-usuario-do-compose>
AGENDA_DB_PASSWORD=<mesma-senha-do-compose>
JWT_SECRET=<segredo-local-longo-e-aleatorio>
```

7. Salve e use **Run**. Para depurar a aplicação com breakpoints, prefira a configuração Spring Boot descrita abaixo.

Não adicione `SPRING_PROFILES_ACTIVE` nessa configuração: `-Ptest` já faz o plugin iniciar o Spring com `test`. Para criar as outras configurações, duplique-a e troque apenas `-Ptest` por `-Phomo` ou `-Pprod`, além das credenciais do ambiente.

Como alternativa, no painel **Maven > Profiles**, marque exatamente um profile e execute **Plugins > spring-boot > spring-boot:run**. Marcar um profile nesse painel só afeta execuções Maven; não altera uma configuração Spring Boot direta.

#### Alternativa recomendada para depuração: configuração Spring Boot

Para breakpoints e hot reload, crie uma configuração **Spring Boot** com JRE 21 e a main class `com.agendamento.smart.AgendaSmartApplication`. Informe `test` no campo **Active profiles** e use as mesmas variáveis de ambiente. Essa modalidade seleciona o profile diretamente no IntelliJ e não usa os profiles Maven do `pom.xml`.

O IntelliJ não herda automaticamente o ambiente do contêiner. Se o `.env` alterar nome, usuário, senha ou porta do Compose, replique esses valores somente na configuração local da IDE. Não salve senhas reais em uma configuração compartilhada do projeto.

### 6. Use PostgreSQL no Docker e a API no IntelliJ

É correto manter API e banco no mesmo `docker-compose.yml`. O Compose aceita iniciar somente um serviço; neste fluxo, suba apenas `db`:

```bash
docker compose up -d --wait db
docker compose ps db
docker compose logs -f db
```

Esse comando não constrói nem inicia a API e não exige `JWT_SECRET`. Como o IntelliJ executa no host, a URL JDBC usa `localhost:5432`. O hostname `db:5432` funciona somente para a API dentro da rede do Compose.

Para esta topologia pequena, indicar o serviço é mais simples do que duplicar arquivos ou introduzir um profile adicional do próprio Compose. Atenção: `docker compose up` sem o nome `db` inicia os dois serviços. Se a API containerizada já estiver em execução, o comando direcionado ao banco não a interrompe; pare-a explicitamente como mostrado abaixo.

O `--wait` confirma que o PostgreSQL passou no healthcheck; o schema e as seeds só são validados/aplicados depois, quando a API do IntelliJ/Maven inicia o Flyway.

Se já existir uma API containerizada, pare-a antes de iniciar no IntelliJ para evitar conflito na porta 8080:

```bash
docker compose stop api
docker compose up -d --wait db
```

Se a porta local 5432 estiver ocupada:

```bash
AGENDA_DB_PORT=5433 docker compose up -d --wait db
```

Nesse caso, configure o IntelliJ com `AGENDA_URL=jdbc:postgresql://localhost:5433/agendamento-smart-api`.

Para parar o banco sem apagar dados:

```bash
docker compose stop db
```

`docker compose down` remove contêineres e rede, mas preserva o volume nomeado `postgresql_data`. Use `docker compose down -v` somente para reinicializar uma base descartável: essa opção apaga definitivamente o banco local do projeto.

### 7. Execute API e banco no Docker

O Compose deste repositório é exclusivo para uso local/teste e ativa `test` por padrão:

```bash
export JWT_SECRET='<segredo-local-longo-e-aleatorio>'
docker compose up -d --build --wait
docker compose ps
docker compose logs -f api
```

Ele utiliza seeds e valores locais; não o promova diretamente para homologação ou produção.

A API containerizada usa `jdbc:postgresql://db:5432/...`, enquanto a API executada no IntelliJ usa `jdbc:postgresql://localhost:5432/...`. O `depends_on` aguarda a saúde do banco apenas no fluxo totalmente containerizado; no fluxo IntelliJ, essa espera é feita por `docker compose up --wait db`.

A imagem do PostgreSQL usa atualmente a tag flutuante `latest`, que pode trocar até a versão principal. Antes de usar um ambiente persistente ou compartilhado, substitua-a por uma tag imutável ou digest homologado e mantenha backup antes de atualizar.

Não execute o profile `prod` no IntelliJ apontando para o banco de produção: a inicialização executa o Flyway e pode aplicar migrations. Produção deve usar pipeline, segredo gerenciado, backup e plano de rollback.

### 8. Construa a imagem da API

O `Dockerfile` executa `mvn clean verify`, de modo que o JAR copiado para a imagem é o mesmo que passou pela etapa de verificação da própria imagem. Construa a imagem uma vez:

```bash
APP_IMAGE='agendamento-smart-api:1.0.5'
docker build --tag "${APP_IMAGE}" .
```

Mantenha a tag alinhada à versão do `pom.xml` ou use o identificador imutável adotado no seu pipeline.

Promova exatamente essa imagem — idealmente pelo digest — de `test` para `homo` e `prod`; não faça um novo build para cada ambiente.

Na execução da imagem, injete as variáveis por um arquivo seguro fora do repositório ou pelo orquestrador:

```bash
docker run --rm --publish 8080:8080 \
  --env-file /caminho/seguro/agendamento-prod.env \
  --env SPRING_PROFILES_ACTIVE=prod \
  "${APP_IMAGE}"
```

Antes de promover para `prod`, valide o mesmo JAR/imagem em `homo`, faça backup do banco e confirme o plano de rollback das migrations.

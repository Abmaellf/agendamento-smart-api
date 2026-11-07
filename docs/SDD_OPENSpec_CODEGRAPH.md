# Desenvolvimento SDD com OpenSpec e CodeGraph

## Objetivo e referência

Este repositório usa OpenSpec para manter contratos observáveis e planejar mudanças antes do código. O padrão foi conferido no projeto `censo` em 2026-11-07, sem modificá-lo: `AGENTS.md`, `openspec/specs`, `openspec/changes` e `.codegraph/.gitignore`. O diretório `.codegraph/` do checkout de referência não continha índice utilizável; a presença do diretório, sozinha, não prova indexação.

As specs atuais são uma **linha de base por inspeção** de código, configuração e migrations deste repositório. Não representam homologação da API nem requisitos futuros. O contrato-alvo do MVP permanece em [REQUISITOS_DO_MVP.md](REQUISITOS_DO_MVP.md). Alguns documentos anteriores em `docs/` descrevem estado mais antigo; quando divergirem, confira o código e abra uma change para corrigir a documentação.

## Ferramentas necessárias

| Ferramenta | Para quê | Estado observado neste ambiente |
|---|---|---|
| Git | Versionar changes, specs e código | 2.55.0 disponível |
| Java 21 | Compilar e executar a API | Corretto 21.0.8 disponível |
| Maven | Build/testes; wrapper `./mvnw` já vem no repositório | Wrapper resolve Maven 3.9.9 |
| Docker + Compose | PostgreSQL local e API em contêiner | Docker 29.8.0 / Compose 2.27.1 disponíveis |
| OpenSpec CLI | Criar/validar/arquivar changes | 1.16.0 instalada em Node v22, fora do `PATH` desta sessão |
| CodeGraph CLI | Indexar e consultar o código | 1.6.2 instalada em Node v22, fora do `PATH` desta sessão; índice local inicializado |
| Node.js | Opcional se usar as versões npm das CLIs | 24.19.0 disponível nesta sessão; npm 11.17.0 |

O OpenSpec também oferece instalador autônomo no Linux: `curl -fsSL https://openspec.dev/cli/install.sh | sh`. Como alternativa, com Node >= 22.12.0: `npm install -g @fission-ai/openspec@latest`. Para CodeGraph: `curl -fsSL https://raw.githubusercontent.com/colbymchenry/codegraph/main/install.sh | sh` ou `npm i -g @colbymchenry/codegraph`. São instalações **por máquina**, não dependências Maven. As versões já instaladas neste ambiente dispensam reinstalação. Consulte [instalação do OpenSpec](https://openspec.dev/docs/installation), [setup OpenSpec](https://openspec.dev/docs/setup) e [guia CodeGraph](https://colbymchenry.github.io/codegraph/getting-started/quickstart/) antes de instalar/atualizar.

Neste ambiente, as CLIs existentes podem ser usadas diretamente:

```bash
/home/abmael/.nvm/versions/node/v22.14.0/bin/openspec --version
/home/abmael/.nvm/versions/node/v22.14.0/bin/codegraph --version
```

Em um terminal que carregue o Node v22 do nvm, `openspec` e `codegraph` devem entrar no `PATH`. O OpenSpec foi inicializado com idioma pt-BR e integração Codex; os seis skills foram gerados em `.agents/skills/`. A escrita desse diretório exigiu permissão adicional neste ambiente, mas a integração está concluída. Em um novo checkout, `openspec init --tools codex --language pt-BR` configura o projeto. O Codex usa `$openspec-propose`, `$openspec-apply-change` e os demais skills gerados.

## Estrutura e ciclo de mudança

```text
AGENTS.md                       instruções de navegação e governança
openspec/config.yaml            contexto do projeto
openspec/specs/<capacidade>/spec.md  contratos da linha de base
openspec/changes/<id>/          proposta e implementação em andamento
openspec/changes/archive/       histórico concluído
.codegraph/                     índice local, se inicializado
```

Antes de programar uma mudança:

```bash
openspec new change nome-da-mudanca
openspec status --change nome-da-mudanca
```

Escreva `proposal.md`, `design.md` quando a decisão técnica exigir, `specs/<capacidade>/spec.md` com deltas `ADDED`/`MODIFIED`/`REMOVED` e `tasks.md`. Revise o plano, depois execute as tarefas. Valide com `openspec validate nome-da-mudanca --strict`, faça build/testes pertinentes, sincronize as specs e arquive a change concluída. Use `openspec --help` e [referência da CLI](https://openspec.dev/docs/cli) para a sintaxe exata da versão instalada. Não edite um registro arquivado para representar uma mudança nova.

As specs iniciais foram criadas diretamente em `openspec/specs/` porque registram o estado anterior à adoção de SDD; elas não afirmam uma change histórica nem um teste não executado.

## CodeGraph neste projeto

O índice **deste repositório** foi criado com `codegraph init -y` em 2026-11-07. Em outro checkout sem índice, execute `codegraph init` na raiz. Isso cria `.codegraph/` e constrói o índice local. `codegraph install` conecta a CLI aos agentes via MCP, mas altera a configuração do agente na máquina e é opcional para uso manual. Depois:

```bash
codegraph status
codegraph explore "AppointmentController AppointmentService create"
codegraph callers "AppointmentService.create"
codegraph impact "AppointmentService.create"
codegraph sync
```

Use `codegraph status` para verificar se existe índice utilizável. O watcher normalmente mantém o grafo atualizado; `sync` serve quando ele estiver desabilitado ou desatualizado. MCP `codegraph_explore` e CLI `codegraph explore` fazem consultas equivalentes. Consulte a [referência CodeGraph](https://colbymchenry.github.io/codegraph/reference/cli/).

## API local e validação

O Compose usa PostgreSQL 18, profile `test` por padrão e seeds de desenvolvimento. Configure `.env` a partir de `.env.example`, incluindo `JWT_SECRET` e senha local. A aplicação fora do Compose precisa de `AGENDA_URL`, `AGENDA_DB_USER`, `AGENDA_DB_PASSWORD` e `JWT_SECRET`.

```bash
./mvnw clean verify
docker compose up -d --wait postgres
AGENDA_URL=jdbc:postgresql://localhost:5432/agendamento-smart-api \
AGENDA_DB_USER=agenda AGENDA_DB_PASSWORD='<senha-local>' \
JWT_SECRET='<segredo-local>' ./mvnw -Ptest spring-boot:run
```

Para executar tudo no Compose: `docker compose up -d --build --wait`. Antes de usar dados reais, revise as permissões dos endpoints legados documentadas nas specs. Não use `docker compose down -v` em base que precise ser preservada.

Nesta preparação, `openspec validate --specs --strict` aprovou as cinco specs, `codegraph status` confirmou 75 arquivos indexados e `./mvnw clean verify` terminou com `BUILD SUCCESS`. O Maven informou `No tests to run`; não houve inicialização da API ou validação integrada com PostgreSQL.

## Cobertura desta linha de base

| Spec | Evidência principal | Limite |
|---|---|---|
| `authentication-and-users` | Controllers, filtro e segurança | Cadastro/listagem legados sem isolamento por clínica |
| `patients` | Controller, service e repository | Listagem pública/global; duplicidade global |
| `clinic-catalog` | Controller e service de catálogo | Consultas apenas; edição de serviço por ADMIN |
| `appointments` | Controller, service, DTOs e migration | Sem transições, recorrência ou remarcação |
| `runtime-and-errors` | POM, YAML, Flyway e advice | Exceções fora do advice sem contrato uniforme |

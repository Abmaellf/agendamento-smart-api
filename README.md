# API de sistema de agendamentos
<!-- https://github.com/Ileriayo/markdown-badges -->

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/postgresql-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![Apache Tomcat](https://img.shields.io/badge/apache%20tomcat-%23F8DC75.svg?style=for-the-badge&logo=apache-tomcat&logoColor=black)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)
![Git](https://img.shields.io/badge/git-%23F05033.svg?style=for-the-badge&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)

##### FUTURAMENTE

![Amazon S3](https://img.shields.io/badge/Amazon%20S3-FF9900?style=for-the-badge&logo=amazons3&logoColor=white)
![AmazonDynamoDB](https://img.shields.io/badge/Amazon%20DynamoDB-4053D6?style=for-the-badge&logo=Amazon%20DynamoDB&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-%234ea94b.svg?style=for-the-badge&logo=mongodb&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-5469d4?style=for-the-badge&logo=stripe&logoColor=ffffff)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-000?style=for-the-badge&logo=apachekafka)
![RabbitMQ](https://img.shields.io/badge/Rabbitmq-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-%23005C0F.svg?style=for-the-badge&logo=Thymeleaf&logoColor=white)
![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=for-the-badge&logo=swagger&logoColor=white)
![Gmail](https://img.shields.io/badge/Gmail-D14836?style=for-the-badge&logo=gmail&logoColor=white)
![cypress](https://img.shields.io/badge/-cypress-%23E5E5E5?style=for-the-badge&logo=cypress&logoColor=058a5e)



## Descrição
Sistema de gestão API - de sistema de agendamentos - destinado a todas as áreas médicas, clínicas de Psicologia, Fisioterapia etc.

## Documentação técnica

- [Índice da documentação](docs/README.md)
- [Arquitetura do Sistema](docs/ARQUITETURA_DO_SISTEMA.md)
- [Objetivo do sistema](docs/OBJETIVO_DO_SISTEMA.md)
- [Requisitos confirmados do MVP](docs/REQUISITOS_DO_MVP.md)
- [Entrevista de descoberta](docs/ENTREVISTA_DE_DESCOBERTA.md)
- Cada pacote relevante em `src/main` possui um `README.md` com responsabilidades, dependências, entradas e débitos locais.

`Objetivo do sistema` e `Arquitetura do Sistema` descrevem o código atual. `Requisitos do MVP` descreve o contrato-alvo confirmado e não deve ser interpretado como funcionalidade já entregue.

## Regras confirmadas do produto

A clínica representa o tenant e será a fronteira de segurança. O modelo-alvo inclui unidade padrão, usuários, pacientes, profissionais, serviços, agendamentos, séries recorrentes e eventos de auditoria. Pacientes e profissionais são compartilhados no tenant; agendamentos pertencem também a uma unidade.

O MVP bloqueia, sem exceção, sobreposição do mesmo paciente, sobreposição do mesmo profissional e capacidade excedida do serviço. Agendamentos preservam duração e preço aplicados, nunca são excluídos e usam os estados `AGENDADO`, `CONFIRMADO`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO`, `FALTA` e `REMARCADO`.

O backend implementa a criação manual de agendamento com unidade, serviço, profissional opcional, snapshots de duração/preço, autoria e estado inicial. A confirmação é serializada por clínica para impedir conflitos de paciente/profissional e estouro de capacidade, inclusive na disputa concorrente pela última vaga; `Idempotency-Key` protege confirmações duplicadas. Recorrência e as demais transições de estado continuam fora dessa entrega.

## Ambiente local integrado validado

O ambiente usado pelos testes da agenda manual é iniciado na raiz deste repositório:

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f api
```

Serviços:

- API: `http://localhost:8080`;
- PostgreSQL: `localhost:5432`, publicado apenas em `127.0.0.1` por padrão;
- front-end irmão: `http://localhost:3000` quando iniciado com `npm run dev -- --host 0.0.0.0`.

O Compose usa `bitnami/postgresql:latest` — PostgreSQL 18.1 no ambiente validado —, mantém os dados no volume nomeado `postgresql_data` e conecta a API por `jdbc:postgresql://db:5432/...`. Como `latest` é uma tag móvel, confirme a versão resolvida antes de promover a imagem. O schema PostgreSQL usa `UUID` nativo para identificadores, `jsonb` para patologias e `timestamptz` (`TIMESTAMP WITH TIME ZONE`) para instantes absolutos de agendamento e auditoria. As migrations são executadas pelo Flyway `11.14.1`.

Por padrão, a aplicação carrega somente `classpath:db/migration`. O Compose habilita deliberadamente os dados de desenvolvimento com:

```text
AGENDA_FLYWAY_LOCATIONS=classpath:db/migration,classpath:db/seed/dev
```

Assim, executar a aplicação fora do Compose não insere seeds, salvo se essa variável for definida explicitamente. As migrations até `V008__create_table_scheduling.sql` criam o schema e a antiga tabela de agendamentos; a `V009` renomeia a estrutura para `APPOINTMENT`. A seed repetível histórica `R__008_seed_scheduling.sql` conserva o nome por compatibilidade de nomenclatura, embora opere sobre `APPOINTMENT`.

> **Atenção na migração:** o volume e os dados do MySQL anterior não são convertidos nem importados automaticamente. `postgresql_data` é um armazenamento separado. Dados que precisem ser preservados exigem um processo explícito de exportação, transformação e importação para PostgreSQL antes da troca definitiva.

Não execute `flyway repair` de forma cega para aceitar o histórico MySQL ou mascarar divergências de checksum. `repair` altera metadados do histórico, mas não converte schema nem dados; antes de qualquer uso, confirme o banco alvo, a origem do histórico e o plano de migração/rollback.

Para reaplicar migrations e seeds integralmente, use apenas uma base descartável. O comando seguinte remove **somente os volumes deste projeto Compose**, incluindo `postgresql_data`, e não deve ser usado quando houver dados PostgreSQL locais a preservar:

```bash
docker compose down -v
docker compose up -d --build
```

Se uma tentativa anterior de criação deixar apenas a API fora da rede `backend`, preserve o volume e recrie só o contêiner efêmero:

```bash
docker compose stop api
docker compose up -d --force-recreate api
```

Validação rápida:

```bash
./mvnw test
```

O roteiro completo de caixa branca e Postman está no front-end, em `specs/001-criar-agendamento-manual/como-executar-testes.md`.

## Instrução de instalação

## Pre requisitos
##### Java Versão 21
##### Maven
##### Docker
##### Docker Compose


## Comandos

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f api
```

Para executar a API pelo Maven fora do Compose, mantenha o PostgreSQL ativo e informe a conexão explicitamente:

```bash
AGENDA_URL=jdbc:postgresql://localhost:5432/agendamento-smart-api \
AGENDA_DB_USER=agenda \
AGENDA_DB_PASSWORD=agenda-local-password \
./mvnw spring-boot:run
```

## Licença

## Contribuição

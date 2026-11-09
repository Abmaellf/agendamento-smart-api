# Pré-verificação da estrutura de clínica e vínculos

## Escopo

Relatório da tarefa 1.1 da change `estrutura-clinica-matriz-vinculos`,
executado em 2026-11-08. A consulta reutilizável está em
[`docs/sql/preflight-estrutura-clinica-matriz-vinculos.sql`](sql/preflight-estrutura-clinica-matriz-vinculos.sql).

A verificação foi executada em uma base PostgreSQL descartável contendo somente
V001--V009 e as seeds de desenvolvimento R__001--R__008. O relatório seleciona
apenas UUIDs técnicos e contagens; nomes, logins e outros dados pessoais não são
consultados.

## Resultado observado com a seed atual

- Clínicas sem unidade: `0`.
- Clínicas com zero ou múltiplos `ADMIN`: `0`.
- A clínica `550e8400-e29b-41d4-a716-446655440000` possui duas candidatas a
  MATRIZ:
  - unidade `60000000-0000-4000-8000-000000000001`, com quatro appointments;
  - unidade `60000000-0000-4000-8000-000000000002`, com dois appointments.
- O profissional `30000000-0000-4000-8000-000000000001` aparece em três
  appointments distribuídos entre as duas unidades.
- O profissional `30000000-0000-4000-8000-000000000002` aparece somente na
  unidade `60000000-0000-4000-8000-000000000001`.

## Decisões exigidas antes da migration

A migration não pode escolher arbitrariamente qual das duas unidades existentes
será `MATRIZ`, nem a unidade exclusiva do profissional usado nas duas unidades.
Antes de tornar `PROFESSIONAL.unit_id` obrigatório, é necessário registrar:

- qual unidade da clínica da seed será `MATRIZ`; a outra será preservada como
  `FILIAL`;
- a qual unidade será vinculado o profissional
  `30000000-0000-4000-8000-000000000001` e como os appointments históricos da
  outra unidade serão preservados.

Para clínicas reais, qualquer linha retornada nas seções 1, 2 ou 4 do script
também exige saneamento ou decisão explícita antes da migration. A seção 3 exige
decisão quando uma clínica possui mais de uma unidade.

## Comando de execução

Com `psql` apontando para a base que será auditada:

```bash
psql --set=ON_ERROR_STOP=1 \
  --file=docs/sql/preflight-estrutura-clinica-matriz-vinculos.sql
```

O script abre uma transação `READ ONLY` e não modifica os dados auditados.

# Design

## Context

Consulte `proposal.md` e os deltas de `clinic-catalog`, `authentication-and-users` e `patients`. A API já carrega `clinicUnit` no principal e aplica a unidade associada em parte da agenda/pacientes, mas `CatalogService` consulta unidades, pacientes e profissionais pela clínica inteira. A listagem de usuários também retorna entidades diretamente e a regra de autorização ainda permite `USER` em `/auth/list`.

## Goals / Non-Goals

**Goals:**

- Centralizar a resolução de unidades permitidas em um único serviço/política reutilizável.
- Aplicar essa resolução a unidades, pacientes, profissionais, usuários e endpoints legados de catálogo.
- Manter ADMIN com escopo da clínica e USER com a unidade associada.
- Usar projeções/DTOs seguros para não devolver senha ou hash em consultas de usuários.

**Non-Goals:**

- Alterar a associação persistida ou a migration da change `relacionar-usuarios-unidades-clinicas`.
- Criar memberships múltiplos, troca de unidade para USER ou novos filtros de texto/paginação além dos contratos existentes.
- Tratar parâmetro `unitId` do cliente como autorização.

## Decisions

### Resolver escopo a partir do principal

Introduzir ou reutilizar uma política de escopo que devolve todas as unidades da clínica para ADMIN e a unidade persistida para USER. Serviços de catálogo consumirão IDs derivados do principal. Duplicar `if` de papel em cada consulta foi rejeitado porque permite divergência entre `/api/units`, pacientes e profissionais.

### Filtrar na consulta ou no serviço antes da serialização

As consultas devem aplicar o conjunto permitido no repositório/serviço antes de montar o DTO. Filtrar depois de carregar a clínica inteira foi rejeitado por transferir dados não autorizados para a aplicação e por piorar o custo em catálogos grandes.

### Manter serviços no escopo da clínica

Serviços não possuem vínculo operacional com unidade no modelo atual; continuarão limitados à clínica. Profissionais, pacientes e unidades têm vínculo de unidade e serão filtrados. A alternativa de inventar associação de serviço nesta change foi rejeitada.

### Listagem de usuários administrativa e segura

`/auth/list` será protegido para ADMIN e retornará uma projeção sem credenciais, limitada à clínica e com filtro de unidade quando o contrato administrativo o exigir. USER não terá endpoint de consulta administrativa. Isso alinha a busca de usuários com a regra de que somente ADMIN gerencia escopo.

### Compatibilidade com o frontend

Os envelopes e nomes de campos existentes serão preservados; somente o conjunto de itens poderá diminuir para USER. O frontend deve interpretar `USER` como `BASIC` e não enviar uma unidade alternativa para ampliar os resultados.

## Risks / Trade-offs

- [Dados legados sem `clinicUnit`] → falhar de forma segura e registrar erro diagnóstico; a migration/associação da change predecessora deve ser pré-requisito de rollout.
- [Repositórios sem método por conjunto de unidades] → adicionar consultas específicas e testes de integração, evitando carregar a clínica inteira como atalho.
- [Clientes antigos esperam lista global] → documentar a quebra de escopo e atualizar o frontend irmão na mesma entrega.
- [Alterar `/auth/list` pode afetar consumidores desconhecidos] → verificar referências no repositório, manter DTO compatível onde possível e cobrir ADMIN/USER/anonimo.

## Migration Plan

1. Confirmar que toda conta USER tem unidade associada e que a política de escopo está disponível.
2. Aplicar a política nos catálogos de unidades, pacientes e profissionais e na listagem de usuários/pacientes legada.
3. Atualizar testes de controller, serviço e integração com duas clínicas e duas unidades.
4. Atualizar documentação dos endpoints e coordenar a entrega com o frontend.

Rollback: reverter apenas o filtro de catálogo se necessário, mantendo a associação de usuário e a proteção de tenant; não remover dados de unidade migrados.

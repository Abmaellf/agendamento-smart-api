# Proposal

## Why

Embora a agenda e a listagem legada de pacientes já tenham regras parciais de unidade, os catálogos HTTP ainda retornam recursos de toda a clínica para qualquer usuário autenticado. Isso permite que um usuário `USER` — representado como `BASIC` no contrato do frontend — veja unidades, pacientes e profissionais fora da unidade associada, contrariando a regra de que somente `ADMIN` pode operar o escopo da clínica.

## What Changes

- Fazer a API derivar o escopo efetivo exclusivamente do principal autenticado: todas as unidades da própria clínica para `ADMIN` e somente a unidade associada para `USER`.
- Restringir `GET /api/units` para que `USER` receba apenas sua unidade associada; `ADMIN` continua recebendo as unidades da própria clínica.
- Aplicar o mesmo escopo aos catálogos de pacientes e profissionais e à listagem legada de pacientes, preservando o filtro por serviço dos profissionais.
- Restringir a consulta de usuários por clínica e unidade autorizadas, reservando a administração do escopo e dos vínculos a `ADMIN`, em coordenação com a change de associação usuário–unidade já aberta.
- Manter serviços e demais recursos sem vínculo operacional com unidade no escopo atual, desde que continuem limitados à clínica autenticada.
- Rejeitar ou ignorar qualquer parâmetro de unidade que tente ampliar o escopo de `USER`, sem expor dados de outra unidade ou clínica.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `clinic-catalog`: catálogos de unidades, pacientes e profissionais passam a respeitar o escopo efetivo do principal.
- `authentication-and-users`: consultas de usuários não podem ser globais nem permitir que `USER` gerencie escopo.
- `patients`: listagens de pacientes passam a retornar somente pacientes da unidade autorizada para `USER`.

## Impact

- API: `CatalogService`, `PatientService`, consulta de usuários, resolução central de escopo e testes de controller/integração.
- Contratos HTTP: conteúdo de `GET /api/units`, `GET /api/patients`, `GET /api/professionals`, `GET /patient/list` e `GET /auth/list` poderá ser menor para `USER`; endpoints não devem aceitar unidade fornecida pelo cliente como prova de autorização.
- Frontend irmão: deve usar os catálogos escopados e desabilitar a edição de unidade para `BASIC`.
- Fora de escopo: criar múltiplos vínculos de unidade por usuário, permitir troca de unidade para `USER`, alterar regras de serviços por unidade, migrations de dados ou redefinir o contrato de associação já planejado na change `relacionar-usuarios-unidades-clinicas`.

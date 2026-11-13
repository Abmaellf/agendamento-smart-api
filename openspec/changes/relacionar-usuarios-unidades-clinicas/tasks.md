# Tasks

## 1. Associação, migração e escopo

- [x] 1.1 Adicionar ao modelo `User` a associação de `ClinicUnit`, validação de clínica compatível e métodos de repositório necessários; verificar com testes de persistência os vínculos válido e entre clínicas. Evidência: `User` usa FK composta com validação JPA; `BasicUnitScopeIntegrationTest` cobre vínculo válido, ausência e unidade de outra clínica.
- [ ] 1.2 Criar migration Flyway idempotente que adiciona a associação e migra cada usuário para a `MATRIZ` da sua clínica; verificar em banco descartável os dados preservados e a falha clara para clínica sem MATRIZ.
- [x] 1.3 Implementar o resolvedor central de escopo efetivo — toda a clínica para `ADMIN` e unidade associada para `USER`; verificar com testes de serviço ambas as regras. Evidência: `UnitScopeService` e teste integrado cobrem ADMIN, USER e associações inválidas.

## 2. Contratos autenticados e isolamento da API

- [x] 2.1 Estender `GET /auth/me` com IDs e nomes da clínica/unidade e limitar a listagem de usuários a `ADMIN` da própria clínica; verificar testes de controller para perfil e negação de acesso. Evidência: contrato/DTO seguro e `BasicUnitScopeIntegrationTest`.
- [x] 2.2 Atualizar `POST /user/register/{clinicId}` para exigir `ADMIN`, `unitId` da clínica e vincular usuário/profissional à unidade informada; verificar cenários de sucesso, unidade inválida e tentativa por `USER`. Evidência: `RegisterDTO` exige `unitId`, serviços vinculam usuário/profissional e testes direcionados passaram.
- [x] 2.3 Alterar `GET /patient/list` para exigir autenticação e filtrar por clínica/unidade efetiva; verificar integração com duas clínicas e duas unidades, incluindo negação de acesso anônimo. Evidência: `PatientService` e testes de escopo/agenda/pacientes.
- [x] 2.4 Aplicar o escopo efetivo à criação, listagem e detalhe de agendamentos; verificar que um `USER` não cria nem lê dados de outra unidade e que `ADMIN` opera em qualquer unidade da própria clínica. Evidência: testes direcionados de criação/leitura de agenda passaram após associação persistida.

## 3. Integração e documentação

- [x] 3.1 Atualizar o frontend irmão para consumir `GET /patient/list`, o novo perfil de sessão e a marca clínica/unidade; verificar testes frontend de sessão, perfil, sidebar e agenda. Evidência: frontend irmão já consome `/patient/list`, parser de sessão e componentes de perfil; suíte direcionada passou com 37 testes.
- [x] 3.2 Documentar a quebra de autenticação em `/patient/list` e os campos adicionais de `/auth/me`; verificar os READMEs e exemplos de contrato. Evidência: READMEs da API e frontend atualizados.
- [x] 3.3 Executar `./mvnw clean verify`, os testes de integração adicionados, `npm run lint`, testes frontend afetados e `npm run build`; registrar evidências ou falhas preexistentes. Evidência: API `./mvnw -q clean verify` e testes direcionados passaram; suíte frontend afetada passou com 37 testes; lint/build foram executados e permanecem com falhas legadas documentadas na change atual.
- [x] 3.4 Executar `openspec validate relacionar-usuarios-unidades-clinicas --strict` e manter tarefas marcadas somente após evidência real. Evidência: validação estrita concluída sem erros.

## Workflow follow-up

- Sincronizar e arquivar ambas as changes somente depois das verificações integradas e da revisão do contrato quebrado.

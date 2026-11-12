# Tasks

## 1. Associação, migração e escopo

- [ ] 1.1 Adicionar ao modelo `User` a associação de `ClinicUnit`, validação de clínica compatível e métodos de repositório necessários; verificar com testes de persistência os vínculos válido e entre clínicas.
- [ ] 1.2 Criar migration Flyway idempotente que adiciona a associação e migra cada usuário para a `MATRIZ` da sua clínica; verificar em banco descartável os dados preservados e a falha clara para clínica sem MATRIZ.
- [ ] 1.3 Implementar o resolvedor central de escopo efetivo — toda a clínica para `ADMIN` e unidade associada para `USER`; verificar com testes de serviço ambas as regras.

## 2. Contratos autenticados e isolamento da API

- [ ] 2.1 Estender `GET /auth/me` com IDs e nomes da clínica/unidade e limitar a listagem de usuários a `ADMIN` da própria clínica; verificar testes de controller para perfil e negação de acesso.
- [ ] 2.2 Atualizar `POST /user/register/{clinicId}` para exigir `ADMIN`, `unitId` da clínica e vincular usuário/profissional à unidade informada; verificar cenários de sucesso, unidade inválida e tentativa por `USER`.
- [ ] 2.3 Alterar `GET /patient/list` para exigir autenticação e filtrar por clínica/unidade efetiva; verificar integração com duas clínicas e duas unidades, incluindo negação de acesso anônimo.
- [ ] 2.4 Aplicar o escopo efetivo à criação, listagem e detalhe de agendamentos; verificar que um `USER` não cria nem lê dados de outra unidade e que `ADMIN` opera em qualquer unidade da própria clínica.

## 3. Integração e documentação

- [ ] 3.1 Atualizar o frontend irmão para consumir `GET /patient/list`, o novo perfil de sessão e a marca clínica/unidade; verificar testes frontend de sessão, perfil, sidebar e agenda.
- [ ] 3.2 Documentar a quebra de autenticação em `/patient/list` e os campos adicionais de `/auth/me`; verificar os READMEs e exemplos de contrato.
- [ ] 3.3 Executar `./mvnw clean verify`, os testes de integração adicionados, `npm run lint`, testes frontend afetados e `npm run build`; registrar evidências ou falhas preexistentes.
- [ ] 3.4 Executar `openspec validate relacionar-usuarios-unidades-clinicas --strict` e manter tarefas marcadas somente após evidência real.

## Workflow follow-up

- Sincronizar e arquivar ambas as changes somente depois das verificações integradas e da revisão do contrato quebrado.

# Tasks

## 1. Resolução de escopo e catálogos

- [x] 1.1 Implementar ou reutilizar um resolvedor central de unidades permitidas por principal — todas as unidades da clínica para `ADMIN` e somente `clinicUnit` para `USER` — e cobrir unidade válida, ausência de associação e outra clínica em testes de serviço. Evidência: `BasicUnitScopeIntegrationTest` cobre ADMIN, USER, associação ausente e unidade de outra clínica.
- [x] 1.2 Aplicar o escopo a `GET /api/units`, `GET /api/patients` e `GET /api/services`, preservando envelope e tenant; verificar com testes de controller que USER recebe apenas a unidade/pacientes permitidos e ADMIN recebe a própria clínica. Evidência: teste integrado cobre os três catálogos para BASIC/ADMIN.
- [x] 1.3 Aplicar o escopo por unidade a `GET /api/professionals?serviceId=...`, mantendo a validação do serviço e o filtro por serviço; verificar profissionais de duas unidades e serviço de outra clínica. Evidência: teste integrado retorna somente o profissional da unidade BASIC e os dois para ADMIN.
- [x] 1.4 Atualizar repositórios/consultas para filtrar antes da serialização, evitando carregar catálogo de toda a clínica para USER; verificar consultas e resultados sem itens de outra unidade. Evidência: consultas SQL e respostas do teste integrado são filtradas por `clinic_id`/`unit_id` antes do mapeamento.

## 2. Usuários e listagem legada de pacientes

- [x] 2.1 Restringir `GET /auth/list` a ADMIN da própria clínica, aplicar eventual filtro de unidade permitido e mapear uma resposta sem senha/hash; verificar ADMIN, USER, anônimo e clínica estrangeira. Evidência: `AuthenticationController` usa `@PreAuthorize`, clínica própria e `UserListResponse`; teste integrado cobre ADMIN, BASIC e anônimo.
- [x] 2.2 Manter `GET /auth/me` com unidade autorizada e confirmar que a identidade não aceita unidade arbitrária enviada pelo cliente; verificar contrato com perfil ADMIN e USER. Evidência: `UserService` exige a associação persistida e o teste BASIC valida `activeUnitId`/`unitName`.
- [x] 2.3 Consolidar `GET /patient/list` com autenticação e escopo por unidade, incluindo paginação e casos sem associação; verificar duas clínicas, duas unidades, visitante e tentativa de ampliação por parâmetros. Evidência: `PatientService` deriva o escopo do principal e o teste cobre duas unidades, anonimato e parâmetro incompatível.
- [x] 2.4 Atualizar READMEs dos controllers, serviços, repositórios e contratos HTTP para registrar a quebra de listagens globais e a equivalência `USER`/`BASIC`; verificar que exemplos não expõem credenciais nem tratam frontend como autorização. Evidência: READMEs de autenticação, pacientes e serviços atualizados.

## 3. Integração entre API e frontend

- [x] 3.1 Atualizar/estender testes de integração da API com ADMIN e USER, duas unidades e duas clínicas, cobrindo catálogos, usuários, pacientes e agenda; verificar que nenhum response cruza unidade ou tenant. Evidência: `BasicUnitScopeIntegrationTest` e `ClinicStructureEndToEndIntegrationTest` passaram, além da suíte direcionada de agenda/pacientes.
- [x] 3.2 Executar `./mvnw clean verify` e registrar falhas preexistentes ou dependências externas separadamente; verificar também os testes direcionados dos controllers alterados. Evidência: `./mvnw -q clean verify` passou; testes direcionados de escopo, cadastro, pacientes e agenda passaram.
- [x] 3.3 Coordenar o contrato com o frontend irmão e executar a suíte frontend afetada, incluindo sessão e diálogo de agendamento, antes da validação final; registrar qualquer incompatibilidade de envelope/campo. Evidência: suíte frontend direcionada passou com 37 testes; contrato `/patient/list`, sessão e unidade BASIC estão alinhados.
- [x] 3.4 Executar `openspec validate restringir-buscas-por-unidade-para-basics --strict` e manter as tarefas sem marcação até os comandos e testes produzirem evidência. Evidência: validação estrita concluída sem erros.

## Workflow follow-up

- Implementar em conjunto com a change `relacionar-usuarios-unidades-clinicas`, que fornece a associação persistida usuário–unidade.
- Arquivar somente após revisão dos contratos, deploy coordenado e validação estrita.

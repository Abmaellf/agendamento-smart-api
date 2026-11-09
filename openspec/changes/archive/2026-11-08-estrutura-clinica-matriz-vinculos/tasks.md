# Tasks

## 1. Preparar dados e constraints

- [x] 1.1 Produzir pré-verificação de clínicas sem unidade, com zero ou múltiplos `ADMIN`, unidades candidatas a MATRIZ e profissionais usados em appointments de mais de uma unidade; verificar o relatório em base descartável com a seed atual e registrar casos que exigem decisão de migração sem expor dados pessoais.
- [x] 1.2 Criar nova migration Flyway para status/tipo, vínculos profissional-unidade/usuário e paciente-unidade, preencher dados legados de forma revisada e impor unicidade de `ADMIN` e MATRIZ por clínica; verificar migration, `ddl-auto=validate` e rejeição de vínculos entre clínicas em PostgreSQL descartável.
- [x] 1.3 Documentar o mapeamento e a pré-verificação de dados legados em `src/main/resources/db/migration/README.md`; verificar que o procedimento preserva a segunda unidade da seed e não altera V001–V009.

## 2. Criar clínica, MATRIZ e equipe inicial

- [x] 2.1 Implementar o serviço transacional que cria clínica/MATRIZ `ATIVO`, ATENDENTE/ADMINISTRATIVO e suas contas com senhas codificadas; verificar teste integrado de criação completa, um único `ADMIN` e rollback total se o segundo login for duplicado.
- [x] 2.2 Expor `POST /auth/register/clinic` com validação dos dados e resposta 201 sem credenciais; verificar teste HTTP de cadastro válido, payload inválido e ausência de senha/hash na resposta.
- [x] 2.3 Documentar o contrato de cadastro inicial e os papéis das contas em documentação de API do projeto; verificar que o exemplo documentado corresponde ao teste HTTP.
- [x] 2.4 Reconciliar `docs/REQUISITOS_DO_MVP.md` com as decisões aprovadas de ativação inicial, administrador único e vínculos por unidade; verificar que o documento distingue o alvo aprovado do comportamento já entregue.

## 3. Vincular usuários, profissionais e pacientes

- [x] 3.1 Adaptar `POST /user/register/{clinicId}` para exigir `isProfessional`, validar a clínica do principal e impedir outro `ADMIN`; verificar testes de `true`/`false`, clínica alheia, login duplicado, senha codificada e rollback conjunto de usuário/profissional.
- [x] 3.2 Adaptar `POST /patient/save` para exigir `unitIds` da clínica autenticada e persistir um ou mais vínculos; verificar testes de uma e duas unidades, lista vazia, unidade alheia e manutenção da regra atual de nome duplicado.
- [x] 3.3 Documentar os novos campos obrigatórios e o vínculo exclusivo de profissional e múltiplo de paciente em documentação de API; verificar que exemplos de request/resposta acompanham os testes HTTP.

## 4. Aplicar as regras à agenda

- [x] 4.1 Bloquear criação de appointment quando clínica ou unidade estiver `INATIVO`/`BLOQUEADA` e validar vínculo do paciente e do profissional informado à unidade; verificar testes de cada status, paciente alheio, profissional de outra unidade e profissional ausente.
- [x] 4.2 Permitir login em todos os três status, negar leitura de appointments em `INATIVO` e permitir em `BLOQUEADA`; verificar testes HTTP de login, busca por ID, filtro por unidade e listagem por clínica integrada à change `corrigir-busca-appointments-clinica`.
- [x] 4.3 Documentar a matriz de permissão de login/criação/leitura por status e a precedência entre clínica e unidade; verificar que os exemplos correspondem aos testes da agenda.

## 5. Verificação integrada

- [x] 5.1 Executar `./mvnw clean verify` e fluxo integrado com PostgreSQL descartável para duas clínicas, equipe inicial, paciente, profissional e appointments; registrar resultados reais e verificar isolamento por clínica, unicidade de ADMIN/MATRIZ e compatibilidade da busca de appointments.

## Workflow follow-up

- Após implementação e verificação, sincronizar os deltas com as specs principais e arquivar a change conforme `docs/SDD_OPENSpec_CODEGRAPH.md`.

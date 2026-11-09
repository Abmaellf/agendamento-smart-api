# Design

## Context

Ver `proposal.md` e os deltas em `specs/`. Hoje `ClinicService.createClinic` persiste apenas `Clinic` e não tem rota HTTP; `UserController` aceita `clinicId` e papel do cliente; `Patient` e `Professional` têm clínica, mas não unidade. `ClinicUnit` não tem tipo/status e `Professional.available_week_days` é obrigatório. `AppointmentService` verifica clínica, mas não status nem pertencimento do paciente/profissional à unidade. As seeds de desenvolvimento já contêm duas unidades. A linha de base OpenSpec registra o código, enquanto `docs/REQUISITOS_DO_MVP.md` descreve um alvo anterior que diverge das novas decisões.

## Goals / Non-Goals

**Goals:**

- Fazer o cadastro inicial ser indivisível e deixar os vínculos de tenant verificáveis também no banco.
- Manter os agendamentos existentes e os endpoints legados utilizáveis enquanto os contratos de cadastro recebem novos campos obrigatórios.
- Definir a precedência dos status na agenda sem bloquear login.

**Non-Goals:**

- Criar FILIAL, modal de seleção, GERENTE, troca de ADMIN ou endpoints de transição de status.
- Alterar o status de appointment ou tornar o profissional obrigatório na criação.
- Classificar tipos de todos os profissionais clínicos legados nesta fase.

## Decisions

### Cadastro inicial transacional

Adicionar `POST /auth/register/clinic` ao fluxo público de cadastro, recebendo clínica, dados da MATRIZ e dados/credenciais distintos do ATENDENTE e do ADMINISTRATIVO. O serviço valida logins globalmente, codifica senhas e cria clínica, MATRIZ, profissionais e contas na mesma transação. Gera IDs no servidor e retorna DTO sem senha ou hash. A rota aproveita o prefixo de cadastro já liberado pela configuração de segurança. Criar só a clínica e completar o restante em requisições posteriores foi descartado porque permitiria clínica sem MATRIZ ou sem ADMIN.

### Invariantes persistentes

Adicionar status à clínica, status e tipo à unidade, `unit_id` obrigatório ao profissional, vínculo opcional e único entre profissional e usuário, e tabela de associação paciente-unidade. Índices únicos condicionais por clínica garantem no máximo um `ADMIN` e uma MATRIZ; a transação de onboarding garante que ambos existam. Chaves compostas ou validação equivalente no banco impedem vínculos entre clínicas. O profissional continua exclusivo de uma unidade; o paciente pode ocupar várias unidades da sua clínica. A alternativa de confiar apenas em validação do serviço foi descartada por concorrência e escritas fora da API.

### Perfis operacionais iniciais

Os dois profissionais automáticos recebem os tipos `ATENDENTE` e `ADMINISTRATIVO` e contas vinculadas; o administrativo da MATRIZ tem papel `ADMIN`, o atendente `USER`. Eles nascem sem serviços de atendimento associados e com lista vazia de dias disponíveis, representável pelo campo legado não nulo. Assim não aparecem no catálogo filtrado por serviço; o vínculo deles a appointments não é definido nesta fase. Atributos de tipo para profissionais clínicos anteriores podem permanecer ausentes até sua classificação específica, sem inventar uma categoria de negócio.

### Cadastros posteriores e isolamento

Manter `POST /user/register/{clinicId}` por compatibilidade, mas comparar o ID do caminho com a clínica autenticada e recusar novo papel `ADMIN`. Exigir `isProfessional`; quando verdadeiro, criar conta e perfil profissional da MATRIZ na mesma transação com os dados requeridos, e quando falso criar só a conta. `POST /patient/save` passa a exigir `unitIds` não vazia, derivar a clínica do principal e validar todos os IDs na mesma clínica. O `clinicId` legado, se presente, é apenas verificado. O comportamento da listagem pública legada de pacientes não é corrigido por esta change e continua um débito explícito da linha de base.

### Status e agenda

O status inicial de clínica e MATRIZ é `ATIVO`; `FILIAL` é um valor previsto para o tipo, sem criação nesta fase. Login não consulta o status como bloqueio. Em appointments, `INATIVO` ou `BLOQUEADA` na clínica ou unidade impede criação; `INATIVO` impede leitura, enquanto `BLOQUEADA` a permite. Na listagem por clínica planejada em `corrigir-busca-appointments-clinica`, registros de unidades inativas são excluídos; filtro explícito ou detalhe de unidade inativa responde 403. A clínica do principal continua sendo a origem do tenant. O profissional informado continua opcional e precisa pertencer à unidade escolhida.

## Risks / Trade-offs

- [Dados existentes podem ter mais de um `ADMIN`, clínica sem unidade ou profissional usado em várias unidades] → Executar pré-verificações e corrigir casos incompatíveis de forma explícita antes de impor constraints; não escolher administradores ou profissionais arbitrariamente.
- [A seed atual tem duas unidades apesar da fase inicial de MATRIZ única] → Preservar ambas; classificar uma como MATRIZ e a outra como FILIAL somente após uma regra de mapeamento revisada, sem apagar histórico nem reescrever migrations já aplicadas.
- [Novos campos obrigatórios quebram clientes antigos] → Versionar/documentar os payloads, validar erros e coordenar a adaptação dos clientes antes da publicação.
- [A change ativa de busca permite lista de todas as unidades] → Integrar seus testes à filtragem de unidades `INATIVO` e reconciliar os deltas antes de sincronizar ou arquivar qualquer uma das changes.
- [O cadastro de equipe operacional precisa coexistir com profissionais clínicos legados] → Aceitar tipo ausente nos registros antigos durante esta fase e não atribuir serviços automaticamente aos operacionais.

## Migration Plan

1. Levantar por clínica administradores, unidades, profissionais, pacientes e vínculos efetivos dos appointments existentes. Definir explicitamente a MATRIZ das clínicas com várias unidades e resolver profissionais usados em mais de uma unidade antes de tornar `unit_id` obrigatório.
2. Criar nova migration Flyway para status/tipos/vínculos/índices, sem editar V001–V009. Atribuir `ATIVO` aos registros legados, preencher associações paciente-unidade a partir dos appointments e vincular pacientes sem appointments à MATRIZ de sua clínica; preservar os dados e a seed histórica.
3. Aplicar a API e testar onboarding atômico, autorização, isolamento, statuses e os dois modos de consulta de appointments. Reconciliar `docs/REQUISITOS_DO_MVP.md` com as novas decisões.
4. Se a aplicação precisar ser revertida, voltar a versão da API; antes de qualquer reversão de schema, avaliar os novos vínculos gravados e preservar os dados. Não executar limpeza de volume como rollback.

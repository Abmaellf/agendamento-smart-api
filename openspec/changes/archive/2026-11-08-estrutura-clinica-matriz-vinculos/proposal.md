# Proposal

## Why

O modelo atual vincula usuários, pacientes e profissionais à clínica, mas não representa os vínculos operacionais por unidade nem cria a estrutura inicial completa de uma clínica. As regras confirmadas nesta conversa exigem uma change própria para estabelecer essas invariantes antes de adaptar os cadastros e os agendamentos.

## What Changes

- Criar uma operação inicial atômica que cadastra clínica `ATIVO`, sua única unidade `MATRIZ` `ATIVO`, um profissional `ATENDENTE` e um `ADMINISTRATIVO`, cada um com conta de usuário e login/senha informados no cadastro. A conta do administrativo da MATRIZ será o único `ADMIN` da clínica.
- Representar `StatusClinic` e `StatusUnit` com `ATIVO`, `INATIVO` e `BLOQUEADA`, e `TypeUnit` com `MATRIZ` e `FILIAL`. Nesta fase, somente a MATRIZ é criada; não há fluxo de criação de FILIAL nem de alteração de status.
- Garantir um único vínculo de clínica por usuário, um único `ADMIN` por clínica, uma única MATRIZ por clínica e um vínculo exclusivo de unidade por profissional; exigir resposta explícita `isProfessional` no cadastro posterior de usuário.
- Associar cada paciente a uma ou mais unidades de sua única clínica e exigir, na criação de appointment, que paciente e profissional informado pertençam à unidade escolhida.
- Permitir login de contas de clínica/unidade `INATIVO` ou `BLOQUEADA`; impedir novos appointments nos dois estados e impedir consultas de appointments quando clínica ou unidade estiver `INATIVO`. `BLOQUEADA` mantém a leitura histórica.
- **BREAKING**: os cadastros posteriores de usuário e paciente passam a exigir, respectivamente, `isProfessional` e `unitIds`; solicitações para outra clínica ou para criar outro `ADMIN` serão rejeitadas.

## Capabilities

### New Capabilities

- `clinic-structure`: cadastro inicial e invariantes de clínica e unidade.
- `professionals`: vínculo de profissional a uma unidade e criação da equipe operacional inicial com contas de usuário.

### Modified Capabilities

- `authentication-and-users`: unicidade de `ADMIN` por clínica e cadastro posterior de usuário com escolha obrigatória de vínculo profissional.
- `patients`: vínculo obrigatório do paciente a uma ou mais unidades da própria clínica no cadastro.
- `appointments`: autorização de criação e leitura conforme status e vínculos com unidade.

## Impact

Afeta contratos HTTP de cadastro inicial, usuários, pacientes e appointments, entidades, repositórios, autorização, migrations Flyway e testes de integração. A change ativa `corrigir-busca-appointments-clinica` continua independente; a leitura por clínica sem `unitId` precisará respeitar os novos bloqueios de unidades inativas ao integrar as duas changes.

O alvo anterior em `docs/REQUISITOS_DO_MVP.md` diverge das decisões atuais quanto à ativação inicial, administradores adicionais e compartilhamento livre de pacientes/profissionais entre unidades; a documentação deverá ser reconciliada na implementação. Ficam fora desta fase o modal de seleção, criação de FILIAL, papel `GERENTE`, troca do `ADMIN`, operações de inativação/substituição de MATRIZ e novas regras de associação obrigatória de profissional antes de `EM_ATENDIMENTO`.

Premissas desta proposta para revisão: a conta do ATENDENTE usa `USER`; `isProfessional=true` no cadastro posterior cria um novo perfil profissional vinculado à conta; os dois profissionais operacionais iniciais nascem sem serviços de atendimento atribuídos.

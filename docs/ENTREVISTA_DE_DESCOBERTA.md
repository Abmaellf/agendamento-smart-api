# Entrevista de descoberta

## Objetivo

Registrar somente dúvidas que permanecem depois da análise do backend, da documentação do frontend e das regras já fornecidas. As respostas serão incorporadas a [Requisitos do MVP](REQUISITOS_DO_MVP.md) e aos READMEs dos módulos afetados.

## Evidências já consideradas

- documentos gerais e READMEs do backend;
- controllers, services, DTOs, entidades, repositories, segurança e migrations atuais;
- entrevista consolidada em `agendamentos-smart/docs/Requisitos do MVP.md`;
- regras complementares fornecidas para conflitos, recorrência, remarcação, auditoria, multi-tenant, snapshots e escopo do MVP.

## Divergências já identificadas no legado

- `Clinic` ainda não funciona como fronteira de segurança;
- não existe `Unit`;
- `Scheduling` não possui serviço, profissional, duração, preço, tenant ou unidade;
- os estados atuais divergem dos estados canônicos;
- não há recorrência, eventos de auditoria, remarcação nem cancelamento;
- exclusão de paciente apaga agendamentos por cascade no banco;
- respostas de usuário podem expor o modelo persistente e a senha derivada;
- horários usam tipos sem fuso explícito.

## Rodadas da entrevista

### 1. Agenda, tempo, disponibilidade e capacidade

Estado: **em entrevista**.

Objetivo: fechar a semântica que define quando um agendamento pode ser criado.

Perguntas da rodada:

1. Qual fuso rege a agenda: um fuso fixo da plataforma, um fuso configurado no tenant ou um fuso por unidade? Ao trocar o fuso configurado, agendamentos antigos mantêm o instante original ou mantêm o horário de parede exibido?
2. A API deve impedir criação no passado? Existe antecedência mínima, horizonte máximo, duração mínima/máxima ou granularidade obrigatória (por exemplo, blocos de 5, 15 ou 30 minutos)?
3. Os dias disponíveis do profissional são uma restrição obrigatória de criação/atribuição ou apenas informação visual? Ao atribuir depois um profissional, o sistema deve revalidar disponibilidade, sobreposição e tenant.
4. Para capacidade, a regra proposta é contar, na mesma unidade e serviço, todos os agendamentos cujos intervalos se sobrepõem, independentemente do profissional. Essa é a regra desejada? Quais estados ainda ocupam vaga: `AGENDADO`, `CONFIRMADO` e `EM_ATENDIMENTO` apenas, ou `FALTA`/`CONCLUIDO` também para consultas históricas de capacidade?

Respostas: pendentes.

### 2. Recorrência, remarcação e cancelamento

Estado: pendente.

Objetivo: definir padrões aceitos, limites, atomicidade e alcance dos comandos sobre séries.

### 3. Cadastros e ciclo de vida

Estado: pendente.

Objetivo: definir edição, inativação, retenção e efeitos sobre agenda para tenant, unidade, usuário, paciente, profissional e serviço.

### 4. Onboarding, autenticação e administração

Estado: pendente.

Objetivo: fechar validação de e-mail, senha temporária, sessão, redefinição de senha e papel master da plataforma.

### 5. Contrato da API e operação

Estado: pendente.

Objetivo: fechar filtros, paginação, formato temporal, idempotência, erros, observabilidade e requisitos de privacidade.

## Registro de respostas

As respostas devem registrar data, decisão, exemplos relevantes e módulos afetados. Uma resposta que alterar uma premissa anterior deve apontar explicitamente qual regra foi substituída.

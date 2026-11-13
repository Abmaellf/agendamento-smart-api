# Spec Delta

## Purpose

Mantém pacotes de sessões por unidade de forma autorizada e reutilizável para a futura contratação e geração automática de agendamentos.

## ADDED Requirements

### Requirement: Pacotes pertencem ao escopo autenticado
A API MUST permitir operações de pacote somente a `ADMIN` e MUST derivar a clínica da identidade autenticada. A unidade e o serviço informados MUST pertencer à mesma clínica; usuários sem autorização ou de outro escopo MUST não consultar nem alterar o recurso.

#### Scenario: Administrador cria pacote da própria unidade
- **WHEN** um administrador envia pacote válido para unidade e serviço de sua clínica
- **THEN** a API MUST criar o pacote e responder com sua definição no escopo autenticado

#### Scenario: Serviço de outra clínica
- **WHEN** a requisição referencia serviço ou unidade fora da clínica autenticada
- **THEN** a API MUST rejeitar a operação sem expor o recurso de outro escopo

### Requirement: Definição persiste dados comerciais e dias únicos
O pacote MUST conter nome, descrição opcional, serviço, unidade, total inteiro positivo de sessões, preço total decimal não negativo, situação e de um a sete dias distintos de domingo a sábado. A frequência semanal MUST ser a contagem dos dias retornados e não um valor persistido ou recebido separadamente.

#### Scenario: Frequência de três dias
- **WHEN** a definição possuir segunda, terça e quarta-feira
- **THEN** a resposta MUST informar frequência semanal igual a três

#### Scenario: Dias duplicados
- **WHEN** a requisição contiver o mesmo dia semanal mais de uma vez
- **THEN** a API MUST rejeitar o corpo sem criar ou atualizar o pacote

### Requirement: A API expõe consulta e mutação sem exclusão física
A API MUST oferecer criação, listagem filtrável, detalhe, atualização e ativação/desativação de pacotes. A listagem MUST aceitar filtros por unidade, serviço, situação, total de sessões e nome; não MUST expor `DELETE`.

#### Scenario: Consulta de pacote inativo
- **WHEN** administrador filtrar ou detalhar pacote inativo de sua clínica
- **THEN** a API MUST retornar sua definição com situação inativa

### Requirement: Validações retornam erro estruturado
Nome ausente, total de sessões inválido, preço negativo, dias ausentes ou inválidos e referências fora do escopo MUST retornar erro estruturado conforme o contrato de erros da API, sem gravação parcial.

#### Scenario: Total de sessões igual a zero
- **WHEN** o administrador enviar `totalSessions` igual a zero
- **THEN** a API MUST responder erro de validação e não persistir o pacote

### Requirement: Pacotes usados são imutáveis
Quando existir vínculo de agendamento de pacote introduzido pela SPEC 02, a API MUST recusar atualização e mudança de situação da definição. Antes desse vínculo existir, a API não MUST criar agendamentos, reservar horário ou exigir profissional.

#### Scenario: Atualização após vínculo futuro
- **WHEN** administrador tentar alterar pacote que possua vínculo de agendamento de pacote
- **THEN** a API MUST responder erro de imutabilidade e preservar a definição existente

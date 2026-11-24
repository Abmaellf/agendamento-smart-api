# Spec Delta

## Purpose

Define o isolamento por unidade dos catálogos e das operações de prévia,
confirmação e consulta de agendamentos originados por pacotes.

## ADDED Requirements

### Requirement: A leitura de pacotes respeita o papel e a unidade
A API MUST permitir a leitura de pacotes ativos para usuários autenticados,
limitando BASIC à unidade associada e ADMIN às unidades da própria clínica.

#### Scenario: BASIC lista pacotes da própria unidade
- **WHEN** um usuário BASIC consultar pacotes ativos
- **THEN** a API MUST retornar somente pacotes ativos da unidade associada à sessão

#### Scenario: BASIC tenta consultar outra unidade
- **WHEN** um usuário BASIC informar uma unidade diferente da sua na consulta
- **THEN** a API MUST rejeitar a consulta sem expor pacotes da outra unidade

#### Scenario: ADMIN consulta unidade da própria clínica
- **WHEN** um ADMIN consultar pacotes ativos informando uma unidade da sua clínica
- **THEN** a API MUST retornar somente pacotes ativos daquela unidade

### Requirement: A contratação exige uma unidade compatível
O request de prévia e confirmação MUST exigir `unitId` e a API MUST validar que
a unidade, pacote e paciente pertencem ao escopo autorizado e à mesma unidade.

#### Scenario: Contratação compatível
- **WHEN** o request informar unidade, pacote ativo e paciente vinculados à mesma unidade autorizada
- **THEN** a API MUST permitir a geração da prévia e preservar essa unidade na resposta

#### Scenario: Paciente de outra unidade
- **WHEN** o paciente informado não estiver vinculado à unidade do pacote
- **THEN** a API MUST rejeitar a operação sem criar contrato ou agendamento

#### Scenario: BASIC informa unidade diferente
- **WHEN** um usuário BASIC informar uma unidade diferente da unidade associada à sessão
- **THEN** a API MUST rejeitar a prévia e a confirmação sem revelar o recurso

### Requirement: A confirmação mantém o contexto da prévia
O fingerprint MUST incluir a unidade efetiva e a confirmação MUST revalidar o
contexto antes de persistir contrato e agendamentos.

#### Scenario: Fingerprint de outra unidade
- **WHEN** a confirmação usar fingerprint calculado para unidade diferente da enviada
- **THEN** a API MUST rejeitar a confirmação sem persistência parcial

### Requirement: A consulta da contratação respeita a unidade autorizada
A API MUST permitir a leitura de uma contratação somente quando sua unidade
estiver no escopo do usuário autenticado.

#### Scenario: BASIC consulta contratação da própria unidade
- **WHEN** um usuário BASIC consultar uma contratação da sua unidade
- **THEN** a API MUST retornar o recurso autorizado

#### Scenario: BASIC consulta contratação de outra unidade
- **WHEN** um usuário BASIC consultar uma contratação de unidade diferente
- **THEN** a API MUST responder como recurso não encontrado ou não autorizado sem expor seus dados

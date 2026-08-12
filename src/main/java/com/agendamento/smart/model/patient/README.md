# Modelo de paciente

## Objetivo do módulo

Representar um paciente pertencente a uma clínica.

## Responsabilidades principais

- Mapear a tabela `PATIENT`.
- Manter UUID, código único, nome e data de criação.
- Exigir associação `ManyToOne` com clínica.
- Participar da geração automática de código.

## Funcionalidades existentes

- Entidade JPA `Patient`.
- UUID gerado pelo provedor JPA.
- Timestamp de criação pelo Hibernate.
- Código atribuído pelo `CodeGeneratorListener`.

## Dependências internas e externas

- Internas: `model/clinic/Clinic` e `model/util/CodeGeneratorListener`.
- Externas: JPA, Hibernate e Lombok.

## Módulos relacionados

`service/PatientService`, `repository`, `mapper`, `model/scheduling` e migration `V003`.

## Pontos de entrada

- Criação e listagem por `PatientService`.
- Consulta por `SchedulingService` ao criar agendamento.

## Fluxos de entrada

`PatientRequestDTO` -> mapper -> associação da clínica -> listener de código -> tabela `PATIENT`; repository -> mapper -> `PatientResponseDTO`.

## Arquivos críticos

- `Patient.java`.
- `../util/CodeGeneratorListener.java`.
- `../../repository/PatientRepository.java`.

## Regras confirmadas para evolução do módulo

- Paciente pertence ao tenant, é compartilhado entre unidades e possui nome, CPF e telefone obrigatórios.
- CPF é único dentro do mesmo tenant; nome não é chave de identidade nem regra de duplicidade.
- O modelo registra `tenantId`, `createdAt`, `updatedAt` e autoria quando aplicável.
- Prontuário, diagnóstico, evolução e documentos clínicos não pertencem ao paciente no MVP.
- Um paciente referenciado por agendamento não pode causar exclusão de histórico; sua política de inativação/anonimização será definida na entrevista.

## Observações técnicas e débitos identificados

- A entidade não possui restrição única de nome; a duplicidade é verificada apenas no service e globalmente.
- A relação com clínica é lazy, mas o mapper acessa `clinic.id`; o fluxo atual mapeia dentro da chamada do repository/service, sem teste que proteja esse comportamento.
- Não existe coleção inversa de pacientes em `Clinic`.
- Não há campos clínicos além do nome; patologias pertencem ao agendamento.

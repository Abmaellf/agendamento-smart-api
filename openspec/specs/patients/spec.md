# Patients Specification

## Purpose

Cadastro e listagem legados observados em `PatientController`, `PatientService`, `PatientRepository` e `PatientMapper`.

## Requirements

### Requirement: Listagem paginada pública

`GET /patient/list` MUST aceitar paginação Spring Data e devolver `content`, `page`, `size`, `totalElements`, `totalPages` e `last`. A configuração atual permite acesso sem autenticação e a consulta não filtra por clínica.

#### Scenario: Visitante lista pacientes

- **WHEN** cliente não autenticado chama `GET /patient/list`
- **THEN** recebe página de pacientes de todas as clínicas

### Requirement: Cadastro de paciente por ADMIN

`POST /patient/save` MUST exigir `ADMIN`, localizar a clínica pelo `clinicId` informado, converter nome para maiúsculas e persistir o paciente. A verificação de nome duplicado é global.

#### Scenario: Novo paciente

- **WHEN** administrador envia nome ainda não encontrado e `clinicId` existente
- **THEN** recebe DTO com ID, nome em maiúsculas e ID da clínica

#### Scenario: Nome já encontrado

- **WHEN** administrador envia nome que já existe na base
- **THEN** o serviço lança `PatientAlreadyExistsException`

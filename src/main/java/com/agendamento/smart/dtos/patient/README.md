# DTOs de paciente

## Objetivo do módulo

Definir os contratos HTTP mínimos para cadastro e retorno de pacientes.

## Responsabilidades principais

- Receber nome e um ou mais UUIDs de unidades.
- Retornar UUID, nome, UUID da clínica derivada da sessão e UUIDs das unidades.

## Funcionalidades existentes

- `PatientRequestDTO` para criação.
- `PatientResponseDTO` para criação e listagem.

## Dependências internas e externas

- Internas: nenhuma dependência de classe do projeto.
- Externas: `java.util.List` e `java.util.UUID`.

## Módulos relacionados

`controller/patient`, `service`, `mapper`, `model/patient` e `model/clinic`.

## Pontos de entrada

- Corpo de `POST /patient/save`.
- Respostas de criação e `GET /patient/list`.

## Fluxos de entrada

JSON -> `PatientRequestDTO` -> serviço com clínica autenticada -> entidade e vínculos de unidade; entidade -> `PatientResponseDTO` -> JSON.

## Arquivos críticos

- `PatientRequestDTO.java`.
- `PatientResponseDTO.java`.

## Regras implementadas

- A clínica é derivada da sessão; `clinicId` é apenas compatibilidade legada e, se informado, deve coincidir.
- `unitIds` é obrigatório e contém uma ou várias unidades da mesma clínica.
- Um paciente pertence a uma única clínica e pode pertencer a várias unidades dela.
- O nome é normalizado em maiúsculas e permanece único globalmente por compatibilidade.

## Observações técnicas e débitos identificados

- `clinicId` deve ser removido do request quando os clientes legados forem migrados.
- O código de paciente e a data de criação existem na entidade, mas não são expostos pelo DTO.

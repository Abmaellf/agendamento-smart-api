# DTOs de paciente

## Objetivo do módulo

Definir os contratos HTTP mínimos para cadastro e retorno de pacientes.

## Responsabilidades principais

- Receber nome e UUID de clínica.
- Retornar UUID, nome e UUID de clínica sem expor a entidade JPA.

## Funcionalidades existentes

- `PatientRequestDTO` para criação.
- `PatientResponseDTO` para criação e listagem.

## Dependências internas e externas

- Internas: nenhuma dependência de classe do projeto.
- Externas: `java.util.UUID`.

## Módulos relacionados

`controller/patient`, `service`, `mapper`, `model/patient` e `model/clinic`.

## Pontos de entrada

- Corpo de `POST /patient/save`.
- Respostas de criação e `GET /patient/list`.

## Fluxos de entrada

JSON -> `PatientRequestDTO` -> service/mapper; entidade -> `PatientResponseDTO` -> JSON.

## Arquivos críticos

- `PatientRequestDTO.java`.
- `PatientResponseDTO.java`.

## Regras confirmadas para evolução do módulo

- Criação exige nome, CPF e telefone; CPF é único no tenant.
- `clinicId` deve sair do comando comum de criação, pois o tenant é derivado da sessão.
- Respostas podem identificar o tenant como contexto, mas não expõem dados pessoais além do necessário para a tela/operação.
- Paciente não possui `unitId`, pois é compartilhado entre unidades do tenant.
- Contratos de edição/inativação serão definidos após a rodada correspondente da entrevista.

## Observações técnicas e débitos identificados

- Não há constraints para nome ou `clinicId`, apesar de o controller usar `@Valid`.
- A clínica vem do cliente e não é derivada do usuário autenticado.
- O código de paciente e a data de criação existem na entidade, mas não são expostos pelo DTO.

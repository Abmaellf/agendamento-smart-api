# Controller de pacientes

## Objetivo do módulo

Expor cadastro e listagem paginada de pacientes.

## Responsabilidades principais

- Adaptar `Pageable` e `PatientRequestDTO` para chamadas ao `PatientService`.
- Entregar `PatientResponseDTO` e `PageResponseDTO`.
- Restringir criação ao papel `ADMIN`.

## Funcionalidades existentes

- `GET /patient/list`: lista todos os pacientes com paginação.
- `POST /patient/save`: cria paciente.

## Dependências internas e externas

- Internas: `PatientService`, DTOs de paciente e paginação.
- Externas: Spring MVC, Spring Data e Spring Method Security.

## Módulos relacionados

`service`, `mapper`, `model/patient`, `model/clinic`, `repository` e `infra/security`.

## Pontos de entrada

- `PatientController.findAllPatient(Pageable)`.
- `PatientController.savePatient(PatientRequestDTO)`.

## Fluxos de entrada

- Lista: parâmetros de página -> service -> repository -> mapper -> wrapper de página.
- Cadastro: corpo JSON -> validação -> service -> resolução da clínica -> persistência -> DTO.

## Arquivos críticos

- `PatientController.java`.
- `../../service/PatientService.java`.

## Regras confirmadas para evolução do módulo

- `ADMIN` e `BASIC` podem cadastrar pacientes do próprio tenant; leitura e manutenção também devem ser isoladas pelo tenant da sessão.
- Nome, CPF e telefone são obrigatórios; CPF é único dentro do mesmo tenant.
- Pacientes são compartilhados entre as futuras unidades do tenant e, portanto, não pertencem a uma unidade específica.
- O request não deve receber `clinicId` como autoridade. O tenant é derivado da sessão.
- Prontuário, diagnóstico, evolução e documentos médicos estão fora do MVP.
- Edição, inativação e política de retenção do paciente permanecem abertas na entrevista.

## Observações técnicas e débitos identificados

- A listagem é pública e não filtra clínica.
- `PatientRequestDTO` não possui constraints; `@Valid` não rejeita nome/clínica nulos antes do service/banco.
- A injeção é por campo, diferente dos controllers mais novos.
- A criação responde 200 em vez de um status específico de criação e escreve em `System.out`.
- As rotas usam verbos (`list`, `save`) e não têm o prefixo `/api` usado em agendamentos.

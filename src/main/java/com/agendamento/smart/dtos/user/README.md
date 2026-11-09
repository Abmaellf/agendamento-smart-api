# DTOs de usuário

## Objetivo do módulo

Definir contratos de cadastro e representação de usuário.

## Responsabilidades principais

- Receber login, senha, papel e a decisão obrigatória `isProfessional`.
- Receber nome e dias disponíveis quando a conta também representar um profissional.
- Representar login, papel e clínica na resposta declarada pelo cadastro.

## Funcionalidades existentes

- `RegisterDTO` é consumido no cadastro e valida condicionalmente os dados profissionais.
- `UserResponseDTO` é o tipo de retorno declarado, mas nenhuma instância é retornada pelo fluxo atual.

## Dependências internas e externas

- Internas: `UserRole` e entidade `Clinic`.
- Externas: nenhuma dependência de framework no contrato atual.

## Módulos relacionados

`controller/user`, `controller/auth`, `mapper`, `model/user` e `model/clinic`.

## Pontos de entrada

- Corpo de `POST /user/register/{clinicId}`.

## Fluxos de entrada

JSON -> `RegisterDTO` -> serviço transacional -> usuário e, opcionalmente, profissional. O endpoint termina com resposta vazia, sem usar `UserResponseDTO` no corpo.

## Arquivos críticos

- `RegisterDTO.java`.
- `UserResponseDTO.java`.

## Regras implementadas

- O cadastro posterior aceita somente `USER`; a conta `ADMIN` única nasce no cadastro da clínica.
- A clínica vem da sessão e deve coincidir com o `clinicId` da rota.
- `isProfessional` é obrigatório; quando verdadeiro, `professional` também é obrigatório.
- O perfil profissional criado pertence exclusivamente à MATRIZ e à mesma clínica do usuário.
- Login é único globalmente sem distinção de caixa; senha nunca aparece na resposta.

## Observações técnicas e débitos identificados

- `UserResponseDTO` contém a entidade `Clinic`, mas o cadastro atual não o expõe.
- A escolha de FILIAL para novos profissionais depende do fluxo futuro de novas unidades.

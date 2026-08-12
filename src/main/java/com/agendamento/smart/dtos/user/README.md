# DTOs de usuário

## Objetivo do módulo

Definir contratos de cadastro e representação de usuário.

## Responsabilidades principais

- Receber login, senha e papel.
- Representar login, papel e clínica na resposta declarada pelo cadastro.

## Funcionalidades existentes

- `RegisterDTO` é consumido no cadastro.
- `UserResponseDTO` é o tipo de retorno declarado, mas nenhuma instância é retornada pelo fluxo atual.

## Dependências internas e externas

- Internas: `UserRole` e entidade `Clinic`.
- Externas: nenhuma dependência de framework no contrato atual.

## Módulos relacionados

`controller/user`, `controller/auth`, `mapper`, `model/user` e `model/clinic`.

## Pontos de entrada

- Corpo de `POST /user/register/{clinicId}`.

## Fluxos de entrada

JSON -> `RegisterDTO` -> mapper/controller -> entidade. O endpoint termina com resposta vazia, sem usar `UserResponseDTO` no corpo.

## Arquivos críticos

- `RegisterDTO.java`.
- `UserResponseDTO.java`.

## Regras confirmadas para evolução do módulo

- O papel canônico operacional é `BASIC`; `USER` é legado e precisa de migração/compatibilidade explícita.
- O comando de criação não aceita clínica arbitrária e só permite papel compatível com a autorização do criador.
- Senha temporária existe apenas no comando apropriado e nunca aparece em respostas.
- Respostas usam IDs/DTOs da clínica e unidade, sem incorporar entidades JPA.
- Sessão/perfil informa validação de e-mail e troca obrigatória de senha sem revelar credenciais.

## Observações técnicas e débitos identificados

- Não há constraints de login, senha ou papel.
- O cliente escolhe diretamente `UserRole`, inclusive `ADMIN`.
- `UserResponseDTO` contém a entidade `Clinic`, acoplando contrato HTTP e persistência.
- `RegisterDTO` possui import de `UUID` não utilizado.

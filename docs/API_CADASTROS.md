# API de cadastros de clínica, usuários e pacientes

Este documento descreve os contratos implementados pela change
`estrutura-clinica-matriz-vinculos`. Durante a change, os artefatos OpenSpec são
a fonte da intenção; após sincronização e arquivamento, as specs principais
passam a registrar o comportamento entregue.

## Cadastro inicial da clínica

### `POST /auth/register/clinic`

Rota pública que cria atomicamente:

- clínica `ATIVO`;
- uma única unidade `MATRIZ` `ATIVO`;
- profissional `ATENDENTE` com conta `USER`;
- profissional `ADMINISTRATIVO` com a única conta `ADMIN` da clínica.

Os dois logins são únicos globalmente, comparados sem diferença entre maiúsculas
e minúsculas. As senhas são obrigatórias, possuem entre 8 e 72 caracteres e são
persistidas somente como hash BCrypt. Os profissionais iniciais nascem sem
serviços e sem dias de atendimento disponíveis.

Exemplo de request, também exercitado pelo teste HTTP:

```json
{
  "clinic": {
    "name": "Clínica HTTP"
  },
  "matrix": {
    "name": "Matriz HTTP",
    "timeZone": "America/Cuiaba"
  },
  "attendant": {
    "name": "Atendente HTTP",
    "login": "attendant-http@example.test",
    "password": "Attendant@123"
  },
  "administrative": {
    "name": "Administrativo HTTP",
    "login": "admin-http@example.test",
    "password": "Administrative@123"
  }
}
```

Resposta `201 Created`:

```json
{
  "clinicId": "11111111-1111-4111-8111-111111111111",
  "clinicStatus": "ATIVO",
  "unitId": "22222222-2222-4222-8222-222222222222",
  "unitStatus": "ATIVO",
  "unitType": "MATRIZ",
  "attendant": {
    "userId": "33333333-3333-4333-8333-333333333333",
    "professionalId": "44444444-4444-4444-8444-444444444444",
    "role": "USER",
    "professionalType": "ATENDENTE"
  },
  "administrative": {
    "userId": "55555555-5555-4555-8555-555555555555",
    "professionalId": "66666666-6666-4666-8666-666666666666",
    "role": "ADMIN",
    "professionalType": "ADMINISTRATIVO"
  }
}
```

A resposta não contém login, senha ou hash. Se qualquer parte falhar, nenhum dos
registros da nova clínica permanece gravado.

Erros relevantes:

| HTTP | `code` | Situação |
|---|---|---|
| `400` | `VALIDATION_ERROR` | Campo obrigatório ausente, vazio ou fora do tamanho permitido |
| `400` | `INVALID_TIME_ZONE` | Fuso da MATRIZ não reconhecido por `ZoneId` |
| `409` | `LOGIN_ALREADY_EXISTS` | Logins iguais entre si ou login já existente na plataforma |

## Cadastro posterior de usuário

### `POST /user/register/{clinicId}`

Rota autenticada que cadastra somente usuários com papel `USER`. O `clinicId`
da rota deve ser o mesmo do usuário autenticado; o cliente não pode criar outro
`ADMIN` nem cadastrar usuário em outra clínica. O login é único globalmente,
sem diferença entre maiúsculas e minúsculas, e a senha é persistida como hash
BCrypt.

O campo booleano `isProfessional` é obrigatório em toda requisição:

- `false`: cria somente a conta de usuário; `professional` pode ser omitido;
- `true`: exige `professional` e cria, na mesma transação, um profissional
  vinculado exclusivamente à MATRIZ da clínica e à nova conta.

Exemplo sem perfil profissional:

```json
{
  "login": "new-user@example.test",
  "password": "NewUser@123",
  "role": "USER",
  "isProfessional": false
}
```

Exemplo com perfil profissional, correspondente ao teste HTTP:

```json
{
  "login": "clinical-professional@example.test",
  "password": "Professional@123",
  "role": "USER",
  "isProfessional": true,
  "professional": {
    "name": "Profissional Clínico",
    "availableWeekDays": [1, 3, 5]
  }
}
```

A resposta é `200 OK` sem corpo. A criação da conta e do profissional é
atômica: se qualquer persistência falhar, nenhum dos dois permanece gravado.
Um profissional pertence a uma única clínica e a uma única unidade; nesta
entrega, novos profissionais são vinculados à MATRIZ porque ainda não há fluxo
de seleção/criação de FILIAL.

Erros relevantes:

| HTTP | `code` | Situação |
|---|---|---|
| `400` | `VALIDATION_ERROR` | `isProfessional` ausente, dados profissionais ausentes/inválidos ou outro campo inválido |
| `403` | `CLINIC_ACCESS_DENIED` | `clinicId` não pertence ao usuário autenticado |
| `403` | `ADMIN_ROLE_NOT_ALLOWED` | tentativa de criar outro `ADMIN` |
| `409` | `LOGIN_ALREADY_EXISTS` | login já utilizado na plataforma |

## Cadastro de paciente

### `POST /patient/save`

Rota autenticada que deriva a clínica da sessão. `unitIds` é obrigatório, não
pode ser vazio e pode conter uma ou várias unidades, desde que todas pertençam
à clínica autenticada. O paciente pertence a uma única clínica, mas pode ser
vinculado a várias unidades dessa clínica.

`clinicId` permanece temporariamente aceito para compatibilidade com clientes
legados. Ele pode ser omitido ou enviado como `null`; quando informado, deve
coincidir com a clínica autenticada e nunca substitui o contexto da sessão.

Exemplo com duas unidades, correspondente ao teste HTTP:

```json
{
  "name": "Paciente Duas Unidades",
  "clinicId": null,
  "unitIds": [
    "22222222-2222-4222-8222-222222222222",
    "77777777-7777-4777-8777-777777777777"
  ]
}
```

Resposta `200 OK`:

```json
{
  "id": "88888888-8888-4888-8888-888888888888",
  "name": "PACIENTE DUAS UNIDADES",
  "clinicId": "11111111-1111-4111-8111-111111111111",
  "unitIds": [
    "22222222-2222-4222-8222-222222222222",
    "77777777-7777-4777-8777-777777777777"
  ]
}
```

O nome é normalizado para maiúsculas. A regra legada de unicidade global do
nome continua vigente nesta entrega.

Erros relevantes:

| HTTP | `code` | Situação |
|---|---|---|
| `400` | `VALIDATION_ERROR` | nome ausente ou `unitIds` vazio/nulo |
| `403` | `CLINIC_ACCESS_DENIED` | `clinicId` legado diverge da clínica autenticada |
| `404` | `TENANT_RESOURCE_NOT_FOUND` | alguma unidade não pertence à clínica autenticada |
| `409` | `PATIENT_ALREADY_EXISTS` | nome de paciente já utilizado |

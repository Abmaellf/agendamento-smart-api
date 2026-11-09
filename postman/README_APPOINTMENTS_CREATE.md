# Criar appointment pelo Postman

Importe o arquivo appointments-create.postman_collection.json no Postman. A
coleção possui uma única requisição: Criar appointment.

## 1. Configure a autenticação

Faça login com um usuário da clínica. Copie o token da resposta para a
variável token da coleção ou mantenha o cookie jwt criado pelo login no
Postman. O script só envia Authorization Bearer quando token estiver
preenchido.

## 2. Configure o ambiente e os IDs

Defina baseUrl, por exemplo http://localhost:8080, e substitua os IDs de
exemplo caso não esteja usando os seeds de desenvolvimento.

| Variável | Obrigatória | Regra |
| --- | --- | --- |
| patientId | Sim | UUID de paciente da clínica autenticada e vinculado à unidade |
| unitId | Sim | UUID de unidade ATIVO da clínica autenticada |
| serviceId | Sim | UUID de serviço da clínica autenticada |
| professionalId | Não | UUID de profissional da mesma unidade que ofereça o serviço |
| durationMinutes | Sim | Inteiro de 1 a 480 |
| price | Sim | Número não negativo com até duas casas; para usuário comum, deve ser igual ao preço do serviço |

Os valores iniciais correspondem aos seeds de desenvolvimento. Eles não devem
ser usados como referência para outros ambientes.

## 3. Escolha a data e a chave de idempotência

Deixe startsAt e idempotencyKey vazios para o script gerar:

- startsAt: sete dias no futuro, às 15:00 UTC;
- idempotencyKey: chave UUID com prefixo appointment-.

Se informar startsAt, use ISO-8601 em UTC, por exemplo
2026-11-18T15:00:00Z. A data precisa estar no futuro e corresponder a um
horário sem conflito.

Mantenha a mesma idempotencyKey apenas ao repetir a mesma tentativa. Para
criar outro appointment, limpe essa variável para gerar uma nova chave.

## 4. Envie a requisição

O corpo é preenchido automaticamente:

~~~
{
  "patientId": "{{patientId}}",
  "unitId": "{{unitId}}",
  "serviceId": "{{serviceId}}",
  "professionalId": null,
  "startsAt": "{{startsAt}}",
  "durationMinutes": 60,
  "price": 150.00
}
~~~

O resultado esperado é 201 Created, com status AGENDADO. A aba Tests verifica
IDs, horário, preço, duração, status e, em reenvio com a mesma chave, que o
mesmo appointment foi devolvido.

## Erros mais comuns

| HTTP | Código | Correção |
| ---: | --- | --- |
| 401/403 | autenticação/autorização | faça login e forneça token ou cookie válido |
| 403 | CLINIC_APPOINTMENTS_DISABLED ou UNIT_APPOINTMENTS_DISABLED | use clínica e unidade ATIVO |
| 404 | TENANT_RESOURCE_NOT_FOUND | use IDs da clínica autenticada |
| 422 | PATIENT_UNIT_MISMATCH | vincule o paciente à unidade escolhida |
| 422 | PROFESSIONAL_UNIT_MISMATCH | use profissional da unidade escolhida |
| 422 | APPOINTMENT_IN_THE_PAST | escolha um startsAt futuro |
| 409 | conflito de paciente, profissional ou capacidade | escolha outro horário, profissional ou serviço |

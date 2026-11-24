# API de agendamento por pacote

Todos os endpoints usam a clínica e o escopo de unidades do principal autenticado:

- `POST /api/package-appointments/preview` valida e simula a contratação;
- `POST /api/package-appointments/confirm` confirma a contratação, exigindo `Idempotency-Key`;
- `GET /api/package-appointments/{id}` consulta uma contratação acessível.

O corpo de preview e confirmação exige `unitId`. Para `USER`, a unidade deve ser a unidade associada à sessão. Para `ADMIN`, deve ser uma unidade da própria clínica. O pacote precisa estar ativo nessa unidade e o paciente precisa estar vinculado a ela; incompatibilidades retornam `TENANT_RESOURCE_NOT_FOUND` sem persistência parcial.

O `unitId` também integra o fingerprint retornado pela prévia, evitando que uma confirmação reutilize uma prévia de outra unidade.

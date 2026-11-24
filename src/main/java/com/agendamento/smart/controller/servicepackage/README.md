# API de pacotes de serviço

A clínica é sempre derivada do principal autenticado. A leitura do catálogo é operacional e pode ser feita por qualquer usuário autenticado; a gestão da definição permanece exclusiva de `ROLE_ADMIN`:

- `GET /api/service-packages` lista com filtros opcionais `unitId`, `serviceId`, `active`, `totalSessions` e `name`. Para `USER`, a resposta é limitada aos pacotes ativos da unidade associada à sessão; uma unidade externa retorna `TENANT_RESOURCE_NOT_FOUND`;
- `POST /api/service-packages` cria uma definição (`ROLE_ADMIN`);
- `GET /api/service-packages/{id}` consulta uma definição (`ROLE_ADMIN`);
- `PATCH /api/service-packages/{id}` atualiza a definição (`ROLE_ADMIN`);
- `PATCH /api/service-packages/{id}/status` ativa ou desativa sem remoção física (`ROLE_ADMIN`).

Não há `DELETE`. Em uma definição com futuros vínculos de agendamento, a SPEC 02 fará o serviço devolver `PACKAGE_IMMUTABLE` (409) para edição e situação.

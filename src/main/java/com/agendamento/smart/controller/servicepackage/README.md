# API de pacotes de serviço

Todos os endpoints exigem `ROLE_ADMIN` e derivam a clínica do principal autenticado:

- `POST /api/service-packages` cria uma definição;
- `GET /api/service-packages` lista com filtros opcionais `unitId`, `serviceId`, `active`, `totalSessions` e `name`;
- `GET /api/service-packages/{id}` consulta uma definição do mesmo escopo;
- `PATCH /api/service-packages/{id}` atualiza a definição;
- `PATCH /api/service-packages/{id}/status` ativa ou desativa sem remoção física.

Não há `DELETE`. Em uma definição com futuros vínculos de agendamento, a SPEC 02 fará o serviço devolver `PACKAGE_IMMUTABLE` (409) para edição e situação.

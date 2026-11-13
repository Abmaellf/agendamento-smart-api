# Tasks

## 1. Persistência e domínio

- [ ] 1.1 Criar migration Flyway para `SERVICE_PACKAGE` e `SERVICE_PACKAGE_WEEK_DAY`, FKs compostas por clínica, checks, índices e unicidade por dia; verificar em PostgreSQL descartável que dias duplicados são recusados.
- [ ] 1.2 Criar entidades, enum, repositório e DTOs de entrada/saída com preço decimal e frequência derivada; verificar testes unitários de conversão e validação.
- [ ] 1.3 Implementar serviço transacional de criação, leitura, filtros, atualização e situação que derive clínica do principal, valide referências e exija `ADMIN`; verificar testes de serviço para escopo, valores e situação.

## 2. Recursos HTTP e proteção

- [ ] 2.1 Expor `POST`, `GET` lista/detalhe, `PATCH` e `PATCH /status` sob `/api/service-packages`, sem `DELETE`; verificar com MockMvc os contratos de sucesso e filtros.
- [ ] 2.2 Devolver erros estruturados para validação, escopo e imutabilidade sem gravação parcial; verificar casos `ADMIN`, BASIC e duas clínicas por testes de integração.
- [ ] 2.3 Preparar a consulta de uso para a SPEC 02, sem criar série ou ocorrência nesta change; verificar que a atualização permanece possível enquanto não houver vínculo de pacote-agendamento.

## 3. Verificação e documentação

- [ ] 3.1 Adicionar cenários de integração PostgreSQL para pacote ativo/inativo, um a sete dias, preço, filtros, autorização e isolamento; executar a classe e registrar o resultado real.
- [ ] 3.2 Atualizar README dos módulos de modelo, repositório, serviço, controller, DTO e migration; verificar que documentam endpoints e a dependência da SPEC 02.
- [ ] 3.3 Executar `./mvnw clean verify` e `openspec validate gerenciar-pacotes-flexiveis --strict`; corrigir falhas introduzidas e registrar falhas preexistentes comprovadas.

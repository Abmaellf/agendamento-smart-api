# Tasks

## 1. Consulta paginada e contrato HTTP

- [x] 1.1 Alterar o repositório de pacotes para consultar por clínica, aplicar filtros opcionais e ordenar por nome com `Pageable`; verificar a assinatura e a consulta por teste de integração ou compilação.
- [x] 1.2 Adaptar serviço e controller para receber `page`/`size`, aplicar defaults e limites, preservar o escopo ADMIN/BASIC e retornar `PageResponseDTO`; verificar páginas, filtros, metadados e erro HTTP 400.
- [x] 1.3 Atualizar os testes de serviço para cobrir defaults/limites de paginação e confirmar que usuário BASIC mantém unidade autorizada e `active=true`.

## 2. Segurança, integração e documentação

- [x] 2.1 Atualizar o teste de integração do endpoint para cobrir primeira/última página, filtros, vazio, parâmetros inválidos e isolamento por clínica/unidade; verificar respostas 200/400/404 e ausência de dados externos.
- [x] 2.2 Atualizar o README do controller com parâmetros, defaults, limite máximo e envelope paginado; verificar que a documentação corresponde ao endpoint implementado.

## 3. Verificação da change

- [x] 3.1 Executar os testes focados de pacotes e corrigir regressões de compilação ou contrato; verificar resultado aprovado.
- [x] 3.2 Executar `./mvnw verify` na API e registrar eventuais bloqueios ambientais; verificar o resultado final do build.
- [x] 3.3 Executar `openspec validate paginar-listagem-pacotes --strict` e confirmar que todos os artefatos e tarefas estão coerentes.

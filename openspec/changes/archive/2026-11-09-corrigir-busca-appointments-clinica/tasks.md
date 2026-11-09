# Tasks

## 1. Reproduzir a falha de busca

- [x] 1.1 Registrar uma reprodução de `GET /api/appointments` com e sem `unitId`, usando `from`/`to` como instantes absolutos e appointments conhecidos em duas unidades; verificar e anotar status, parâmetros e IDs retornados, sem dados pessoais, para distinguir erro de entrada, filtro de unidade e intervalo sem registros.

## 2. Implementar a consulta por clínica

- [x] 2.1 Tornar `unitId` opcional em `AppointmentController.findAll` e rejeitar `from >= to` com HTTP 400; verificar em teste HTTP que `from`/`to` continuam obrigatórios e que intervalo inválido é rejeitado.
- [x] 2.2 Acrescentar consulta de `AppointmentRepository` por clínica e intervalo ordenada por início; verificar em teste de repository ou integração que o limite inicial inclui, o final exclui e as duas unidades da mesma clínica aparecem.
- [x] 2.3 Selecionar em `AppointmentService.findAll` a consulta por clínica quando não houver `unitId`, preservando a validação `(unitId, clinicId)` e o filtro por unidade quando houver; verificar com testes de ambas as modalidades e `content` vazio em intervalo sem registros.

## 3. Verificar autorização e integração

- [x] 3.1 Testar listagem autenticada com duas clínicas: sem `unitId` não retorna registros da outra clínica e `unitId` alheio responde `TENANT_RESOURCE_NOT_FOUND`/404; verificar também que busca por ID alheio continua em 404.
- [x] 3.2 Executar `./mvnw clean verify` e uma consulta HTTP integrada com duas unidades da mesma clínica, registrar resultados observados e comparar com a reprodução inicial; verificar que chamadas existentes com `unitId` mantêm o contrato.

## Workflow follow-up

- Após implementação aprovada e testes concluídos, sincronizar a spec principal e arquivar a change conforme o fluxo SDD do projeto.

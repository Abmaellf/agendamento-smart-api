# Tasks

## 1. Contrato e autorização

- [x] 1.1 Tornar `unitId` obrigatório no request de pacote e aplicar validação de unidade, pacote e paciente no serviço; verificar com testes unitários os cenários ADMIN, BASIC e unidade incompatível.
- [x] 1.2 Separar a autorização do GET de pacotes das mutações administrativas e restringir a leitura BASIC a pacotes ativos da sua unidade; verificar com teste MockMvc de isolamento.
- [x] 1.3 Incluir `unitId` no fingerprint e proteger a consulta da contratação pelo escopo da unidade; verificar rejeição sem persistência parcial.

## 2. Testes e documentação

- [x] 2.1 Adicionar testes de serviço para contratação por pacote e leitura filtrada de pacotes, cobrindo paciente de outra unidade e pacote externo.
- [x] 2.2 Atualizar READMEs da API e do controller de pacotes com o contrato `unitId` e a distinção entre leitura operacional e gestão ADMIN; verificar a documentação contra os endpoints.
- [x] 2.3 Executar `./mvnw clean verify` e `openspec validate selecionar-unidade-agendamento-por-pacote --strict`, registrando resultados reais.

## Workflow follow-up

- Integrar esta change com `agendamento-automatico-por-pacotes` antes da publicação conjunta com o frontend.
- Arquivar somente após os testes de integração PostgreSQL e a revisão do contrato HTTP.


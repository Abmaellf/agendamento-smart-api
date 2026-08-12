# Documentação técnica e de produto

Este diretório separa três perspectivas que não devem ser confundidas:

- [Objetivo do sistema](OBJETIVO_DO_SISTEMA.md): capacidades realmente executáveis no código atual;
- [Arquitetura do Sistema](ARQUITETURA_DO_SISTEMA.md): estrutura, dependências, riscos e estado técnico observado;
- [Requisitos do MVP](REQUISITOS_DO_MVP.md): comportamento confirmado para a evolução do produto e da API;
- [Entrevista de descoberta](ENTREVISTA_DE_DESCOBERTA.md): decisões ainda abertas e respostas que deverão atualizar os requisitos.

Quando houver divergência, código e migrations descrevem o estado atual, enquanto `REQUISITOS_DO_MVP.md` descreve o estado-alvo. Uma regra só pode ser marcada como implementada depois de existir no fluxo executável, na autorização, na persistência quando aplicável e nos testes.

## Manutenção

Toda mudança deve atualizar o README do pacote afetado. Mudanças transversais também atualizam arquitetura, objetivo ou requisitos. Respostas da entrevista devem primeiro ser registradas em `ENTREVISTA_DE_DESCOBERTA.md` e depois incorporadas aos requisitos e aos READMEs relacionados.

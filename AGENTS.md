# Orientações para agentes

## Fontes de verdade

Para intenção: instrução atual do usuário, change OpenSpec ativa, `openspec/specs/`, documentação em `docs/`, código. Para afirmar comportamento entregue: testes executados, código e configuração, specs de base, documentação. Identifique inferências e divergências. `docs/REQUISITOS_DO_MVP.md` descreve o alvo, não o estado implementado.

## Fluxo SDD

1. Leia `README.md`, `docs/SDD_OPENSpec_CODEGRAPH.md` e a spec da capacidade afetada.
2. Antes de localizar símbolos ou fluxos, use CodeGraph se houver índice utilizável em `.codegraph/`; use busca textual para docs, configuração ou quando não houver índice.
3. Para nova mudança de comportamento, contrato, dependência, migration ou arquitetura, crie change em `openspec/changes/<id>/` com proposta, design quando necessário, spec delta e tarefas antes da implementação.
4. Valide a change, implemente as tarefas, execute testes relevantes, sincronize as specs e arquive a change concluída. Registre resultados realmente observados.
5. Preserve specs e changes arquivadas. Mudanças posteriores exigem nova change.

Não ajuste a spec apenas para fazer o código parecer correto. Registre divergências e trate correções em change própria. Preserve modificações preexistentes e não registre segredos ou dados pessoais em artefatos.

## Escopo atual

As specs em `openspec/specs/` são linha de base obtida por inspeção em 2026-11-07. Não constituem evidência de teste integrado. O catálogo e a agenda usam a clínica do usuário autenticado; os endpoints legados de usuários e pacientes têm regras diferentes, descritas nas specs.

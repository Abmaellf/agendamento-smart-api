# Utilitário do modelo

## Objetivo do módulo

Gerar códigos numéricos para clínicas e pacientes antes da persistência JPA.

## Responsabilidades principais

- Detectar se a entidade recebida é `Clinic` ou `Patient`.
- Consultar o maior código persistido.
- Iniciar clínica em `1000` e paciente em `5000`, ou incrementar o maior código.

## Funcionalidades existentes

- Callback `@PrePersist` compartilhado pelas duas entidades.
- Injeção dos repositories em campos estáticos por método `@Autowired`.

## Dependências internas e externas

- Internas: entidades `Clinic`/`Patient` e seus repositories.
- Externas: JPA lifecycle, Jakarta Transactions e Spring DI.

## Módulos relacionados

`model/clinic`, `model/patient` e `repository`.

## Pontos de entrada

- JPA chama `generateCode(Object)` antes de inserir uma entidade anotada com `@EntityListeners(CodeGeneratorListener.class)`.

## Fluxos de entrada

Persistência de entidade sem código -> query `MAX(code)` -> incremento/default -> atribuição -> insert.

## Arquivos críticos

- `CodeGeneratorListener.java`.

## Regras confirmadas para evolução do módulo

- Identificadores auxiliares devem ser gerados de forma atômica e segura sob concorrência; `MAX + 1` não sustenta essa regra.
- Geração de código não pode consultar repositories a partir de entidade/callback JPA.
- Códigos legíveis não substituem UUIDs nem participam da fronteira de tenant.
- Toda estratégia nova deve possuir constraint/migration e teste concorrente compatível com MySQL.

## Observações técnicas e débitos identificados

- O modelo depende da camada de repository, invertendo a direção esperada das camadas.
- Estado estático e injeção Spring em listener JPA tornam ciclo de vida e testes frágeis.
- `MAX + 1` possui condição de corrida; a constraint única apenas transforma a colisão em erro.
- A anotação transacional em callback não define uma estratégia confiável de concorrência.

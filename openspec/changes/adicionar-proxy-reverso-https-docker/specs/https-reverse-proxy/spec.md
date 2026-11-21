# Spec Delta

## Purpose

Define uma entrada pública HTTPS para a API containerizada, mantendo a aplicação
e o PostgreSQL isolados da internet e preservando um modo local sem dependência
de domínio ou certificados públicos.

## ADDED Requirements

### Requirement: A API pública usa HTTPS

O deploy de produção MUST atender o domínio da API por HTTPS válido e MUST redirecionar requisições HTTP para a mesma URL em HTTPS.

#### Scenario: Requisição HTTP é redirecionada

- **WHEN** um cliente acessa o domínio público pela porta 80
- **THEN** recebe redirecionamento para o mesmo recurso na porta 443, sem encaminhamento funcional por HTTP

#### Scenario: Requisição HTTPS chega à API

- **WHEN** um cliente acessa uma rota pública ou autenticada pelo domínio HTTPS com certificado válido
- **THEN** a resposta da API é entregue pelo proxy sem exigir que o cliente conheça a porta interna 8080

### Requirement: As portas internas ficam isoladas

O deploy de produção MUST publicar no host somente as portas 80 e 443 pelo proxy; a API e o PostgreSQL MUST permanecer acessíveis apenas pelas redes Docker internas.

#### Scenario: Inspeção dos serviços publicados

- **WHEN** o operador consulta os serviços e portas publicados do Compose de produção
- **THEN** somente o proxy possui mapeamentos para o host e os serviços `api` e `postgres` não publicam 8080 ou 5432

#### Scenario: Tentativa de contornar o proxy

- **WHEN** um cliente externo tenta conectar diretamente às portas 8080 ou 5432 do servidor
- **THEN** a conexão não alcança a API nem o PostgreSQL

### Requirement: Certificados e estado TLS são persistentes

O proxy MUST obter e renovar certificado para o domínio configurado quando as condições públicas de DNS e firewall forem atendidas, e MUST persistir certificados e estado necessários fora do ciclo de vida do contêiner.

#### Scenario: Proxy é recriado

- **WHEN** o contêiner do proxy é recriado sem remover seus volumes persistentes
- **THEN** o certificado e o estado TLS continuam disponíveis para atender HTTPS sem depender de um novo armazenamento dentro da imagem

### Requirement: O backend reconhece a requisição original

O encaminhamento HTTPS MUST preservar o contexto externo necessário para a aplicação reconhecer esquema e host originais, sem permitir que clientes externos falsifiquem esse contexto por acesso direto à API.

#### Scenario: Login por HTTPS emite cookie seguro

- **WHEN** um usuário válido autentica pela URL HTTPS pública em produção
- **THEN** a resposta de login contém o cookie JWT com `Secure` e `HttpOnly`, e o cookie pode ser enviado em uma requisição autenticada posterior pela mesma origem pública

### Requirement: O modo local não depende de ACME

O cenário local/teste MUST continuar permitindo iniciar a API por HTTP em `localhost:8080` sem exigir DNS público, certificado ACME ou exposição das portas 80 e 443.

#### Scenario: Execução local sem proxy

- **WHEN** o desenvolvedor inicia o Compose de teste sem configuração de domínio público
- **THEN** a API pode ser acessada por HTTP local na porta 8080 e o banco continua disponível conforme o fluxo local documentado


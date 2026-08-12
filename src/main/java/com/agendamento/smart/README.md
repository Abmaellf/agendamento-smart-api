# Pacote raiz da aplicação

## Objetivo do módulo

Concentrar o bootstrap do serviço e delimitar o pacote raiz usado pelo component scan do Spring Boot.

## Responsabilidades principais

- Iniciar o contexto Spring por `AgendaSmartApplication.main`.
- Descobrir automaticamente controllers, services, repositories, configurações, mappers e entidades abaixo de `com.agendamento.smart`.

## Funcionalidades existentes

- Inicialização de uma aplicação web Spring Boot única.
- Autoconfiguração das integrações presentes no classpath: MVC, Security, JPA, Validation e Flyway.

## Dependências internas e externas

- Internas: todos os subpacotes são alcançados pelo component scan; não há import direto no bootstrap.
- Externas: `spring-boot-autoconfigure` e `spring-boot`.

## Módulos relacionados

`controller`, `service`, `repository`, `mapper`, `model`, `dtos`, `infra` e `util`.

## Pontos de entrada

- `AgendaSmartApplication.main(String[])`.

## Fluxos de entrada

O processo Java chama `SpringApplication.run`; o Spring monta o contexto, aplica migrations, cria o acesso ao banco, a cadeia de segurança e os endpoints HTTP.

## Arquivos críticos

- `AgendaSmartApplication.java`: classe principal e raiz do component scan.
- `../../../../../../../resources/application.properties`: configuração do runtime.
- `../../../../../../../../pom.xml`: build e dependências.

## Regras confirmadas para evolução do módulo

- O código atual e o contrato-alvo devem permanecer distinguíveis; [Requisitos do MVP](../../../../../../docs/REQUISITOS_DO_MVP.md) é a referência de produto.
- A inicialização deve validar migrations e configurações necessárias para o isolamento por tenant antes de servir tráfego.
- Componentes de domínio devem continuar dentro do package scan sem deslocar regras de negócio para a classe de bootstrap.

## Observações técnicas e débitos identificados

- O projeto é um único módulo Maven; os subpacotes são limites organizacionais, não limites de compilação.
- Não existem testes de inicialização ou fontes em `src/test`.
- Consulte `../../../../../../../../docs/ARQUITETURA_DO_SISTEMA.md` para as regras transversais.

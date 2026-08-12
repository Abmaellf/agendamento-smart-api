# Modelo de clínica

## Objetivo do módulo

Representar a organização à qual usuários e pacientes são vinculados.

## Responsabilidades principais

- Mapear a tabela `CLINIC`.
- Manter UUID, código único, nome e data de criação.
- Declarar a relação inversa com usuários.
- Participar da geração automática de código antes do insert.

## Funcionalidades existentes

- Entidade JPA `Clinic` com builder e accessors Lombok.
- Código calculado pelo `CodeGeneratorListener`; callback próprio usa o timestamp apenas se o código continuar nulo.
- Lista de usuários ignorada pela serialização Jackson.

## Dependências internas e externas

- Internas: `model/user/User` e `model/util/CodeGeneratorListener`.
- Externas: JPA, Hibernate, Jackson e Lombok.

## Módulos relacionados

`repository`, `service/ClinicService`, `model/patient`, `model/user` e migration `V001`.

## Pontos de entrada

- Persistência por `ClinicRepository`.
- Busca pelo cadastro de usuário/paciente.
- Criação programática por `ClinicService`, sem consumidor encontrado.

## Fluxos de entrada

Builder/service ou materialização JPA -> callbacks de persistência -> tabela `CLINIC`.

## Arquivos críticos

- `Clinic.java`.
- `../util/CodeGeneratorListener.java`.
- `../../repository/ClinicRepository.java`.

## Regras confirmadas para evolução do módulo

- `Clinic` representa o tenant/conta do cliente e é a fronteira de segurança, não uma unidade física.
- O onboarding cria o tenant, uma `Unit` padrão e o primeiro `ADMIN` em uma única operação consistente.
- CPF/CNPJ do contratante é único em toda a plataforma.
- Usuários e serviços pertencem ao tenant; pacientes e profissionais são compartilhados entre suas unidades; agendamentos pertencem também a uma unidade.
- Todo registro do tenant possui timestamps e, quando aplicável, autoria.
- Operação real de múltiplas unidades fica fora do primeiro MVP, mas `Unit` e os vínculos necessários já fazem parte do modelo mínimo.

## Observações técnicas e débitos identificados

- Não existe controller de clínica.
- A migration insere uma clínica fixa; esta é a única criação comprovadamente alcançável no projeto atual.
- Há duas estratégias de código (`MAX + 1` e timestamp), embora normalmente o listener preencha antes do callback da entidade.
- `MAX + 1` não é seguro sob concorrência.
- A entidade declara `@JsonIgnore` e o lado usuário usa `@JsonManagedReference`; as anotações não formam um par consistente.

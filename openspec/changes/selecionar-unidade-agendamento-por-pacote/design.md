# Design

## Context

O contrato de pacote já persiste unidade no `PACKAGE_CONTRACT` e o
`AppointmentService` valida unidade, paciente e escopo na criação individual.
Porém, `PackageAppointmentRequest` não recebe `unitId`,
`PackageAppointmentService` usa apenas o tenant para validar pacote/paciente e
`ServicePackageController` aplica ADMIN à classe inteira.

## Goals / Non-Goals

**Goals:**

- Reutilizar `UnitScopeService` como fonte da unidade autorizada.
- Exigir o mesmo `unitId` no catálogo operacional e na contratação.
- Manter mutações administrativas de pacote protegidas.
- Garantir que o fingerprint represente o contexto de unidade.

**Non-Goals:**

- Criar endpoint de troca de unidade ou mudar a sessão.
- Alterar migrations, entidades ou o fluxo manual de agendamento.
- Fazer autorização depender do filtro enviado pelo frontend.

## Decisions

### Separar leitura operacional das mutações administrativas

O `GET /api/service-packages` será acessível a usuários autenticados, mas o
serviço forçará BASIC à sua unidade e a pacotes ativos. POST, PATCH, status e
detalhe administrativo continuarão exigindo ADMIN.

Alternativa rejeitada: remover a proteção da classe sem impor regras no serviço,
pois isso exporia pacotes de toda a clínica a BASIC.

### Validar a unidade antes de carregar recursos

O serviço de pacote usará `UnitScopeService.allowedUnits` para validar a unidade
solicitada. Em seguida, buscará o pacote no tenant e exigirá que sua unidade
seja a unidade solicitada; o paciente será carregado com suas unidades e
validado contra o mesmo identificador.

Alternativa rejeitada: aceitar a unidade derivada do pacote sem conferir o
request, pois permitiria que a prévia e o fingerprint ocultassem um contexto
divergente do formulário.

### Reutilizar o fingerprint existente com unidade adicionada

A unidade será incluída na entrada do SHA-256 já usado na prévia. A confirmação
continua recomputando a prévia antes de criar o contrato, sem migration.

## Risks / Trade-offs

- [Clientes antigos omitem `unitId`] → tratar como contrato incompatível e
  atualizar o frontend; não inferir silenciosamente uma unidade para ADMIN.
- [BASIC consultar `active=false`] → forçar `active=true` no serviço,
  independentemente do parâmetro recebido.
- [Unidade do paciente mudar entre prévia e confirmação] → revalidar em cada
  chamada dentro da transação de confirmação.

## Migration Plan

1. Publicar o novo DTO e as validações de escopo.
2. Publicar a leitura operacional de pacotes e atualizar documentação.
3. Executar testes unitários e integração PostgreSQL existentes.
4. Não há migration; contratos antigos já persistem `unit_id`.


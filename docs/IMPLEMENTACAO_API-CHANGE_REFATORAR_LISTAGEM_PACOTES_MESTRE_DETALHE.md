# Implementação da Change `refatorar-listagem-pacotes-mestre-detalhe`

## 1. Objetivo

Este documento descreve a implementação completa da mudança de listagem de
pacotes nos dois repositórios do produto:

- frontend React/TypeScript: `agendamento-smart-web`;
- backend Java 21/Spring Boot/PostgreSQL: `agendamento-smart-api`.

No backend, a parte contratual foi organizada na change OpenSpec
`paginar-listagem-pacotes`, pois a refatoração do frontend depende de uma
resposta paginada real.

O resultado é uma experiência mestre-detalhe:

```text
Tabela de pacotes (mestre)
        │ seleciona uma linha
        ▼
Painel contextual (detalhe)

Novo pacote / Editar pacote
        │
        ▼
Modal único de criação e edição
```

A consulta passa a carregar somente a página solicitada e os filtros são
aplicados no banco. O domínio do pacote, as mutations e as regras de acesso
continuam no backend.

## 2. Escopo da mudança

### Entregue

- paginação server-side da listagem;
- filtros de nome, unidade, serviço, situação e sessões;
- ordenação ascendente por nome;
- resposta com conteúdo e metadados de página;
- manutenção do isolamento por clínica;
- manutenção da regra BASIC: unidade autorizada e pacotes ativos;
- tabela no frontend em lugar de cards;
- seleção de registro e detalhe contextual;
- modal único de criação/edição;
- container mais largo e layout responsivo;
- testes unitários e de integração;
- documentação de endpoint e feature.

### Fora de escopo

- migration de banco;
- novos campos do pacote;
- alteração de autorização;
- exclusão física;
- alteração de imutabilidade;
- alteração de criação, edição, detalhe ou status além do envelope da lista;
- paginação de outros catálogos;
- inclusão de nova biblioteca de tabela ou estado global.

## 3. Situação anterior do backend

### 3.1 Controller antigo

Antes, o endpoint devolvia `CatalogResponse` com uma lista:

```java
@GetMapping
public CatalogResponse<ServicePackageResponse> findAll(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) UUID unitId,
        @RequestParam(required = false) UUID serviceId,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Integer totalSessions,
        @RequestParam(required = false) String name) {
    return new CatalogResponse<>(
            service.findAll(user, unitId, serviceId, active, totalSessions, name));
}
```

O endpoint não tinha `page` nem `size`. A ausência desses parâmetros era
compatível com o frontend anterior, mas não permitia navegar de maneira
confiável quando a quantidade de pacotes crescesse.

### 3.2 Serviço antigo

O serviço consultava a lista completa da clínica e executava os filtros em
memória:

```java
Stream<ServicePackageDefinition> packages = packageRepository
        .findAllByClinicIdOrderByNameAsc(user.getClinic().getId())
        .stream();

if (requestedUnitId != null) {
    packages = packages.filter(item ->
            item.getUnit().getId().equals(requestedUnitId));
}
if (serviceId != null) {
    packages = packages.filter(item ->
            item.getService().getId().equals(serviceId));
}
if (requestedActive != null) {
    packages = packages.filter(item ->
            item.isActive() == requestedActive);
}
if (totalSessions != null) {
    packages = packages.filter(item ->
            item.getTotalSessions().equals(totalSessions));
}
if (name != null && !name.isBlank()) {
    String normalized = name.trim().toLowerCase();
    packages = packages.filter(item ->
            item.getName().toLowerCase().contains(normalized));
}

return packages.map(this::toResponse).toList();
```

Esse desenho tinha três problemas:

1. trazia todos os registros para a JVM;
2. filtrava depois da leitura;
3. não tinha `totalElements`, `totalPages` ou `last` confiáveis.

### 3.3 Repositório antigo

O método antigo era uma busca de lista:

```java
List<ServicePackageDefinition> findAllByClinicIdOrderByNameAsc(
        UUID clinicId);
```

Ele expressava apenas a clínica e a ordenação. Unidade, serviço, status,
sessões e nome eram filtrados fora do banco.

## 4. Contrato novo da API

### 4.1 Endpoint

```http
GET /api/service-packages
```

### 4.2 Parâmetros

| Parâmetro | Tipo | Default | Regra |
|---|---|---:|---|
| `page` | `int` | `0` | zero-based, mínimo `0` |
| `size` | `int` | `10` | mínimo `1`, máximo `100` |
| `unitId` | `UUID` | ausente | filtro de unidade |
| `serviceId` | `UUID` | ausente | filtro de serviço |
| `active` | `Boolean` | ausente | filtro de situação |
| `totalSessions` | `Integer` | ausente | filtro de sessões |
| `name` | `String` | ausente | busca parcial por nome |

Exemplo:

```http
GET /api/service-packages?page=0&size=10&active=true&name=fisio
Authorization: Bearer <token>
```

### 4.3 Resposta nova

O envelope é:

```json
{
  "content": [
    {
      "id": "package-1",
      "unitId": "unit-1",
      "unitName": "Matriz",
      "serviceId": "service-1",
      "serviceName": "Fisioterapia",
      "name": "Fisioterapia 10 sessões",
      "description": "Recuperação funcional",
      "totalSessions": 10,
      "totalPrice": "400.00",
      "weekDays": ["MONDAY", "WEDNESDAY"],
      "weeklyFrequency": 2,
      "active": true
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

Os nomes foram escolhidos para corresponder ao adaptador TypeScript
`ServicePackagePage`:

```ts
export interface ServicePackagePage {
  content: ServicePackage[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
```

## 5. Implementação do backend passo a passo

### Passo 1 — reutilizar `PageResponseDTO`

O projeto já possuía um DTO genérico para páginas:

```java
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
```

Não foi criado um DTO duplicado para pacotes. O método `from` copia os valores
calculados pelo Spring Data:

- `getContent()` vira `content`;
- `getNumber()` vira `page`;
- `getSize()` vira `size`;
- `getTotalElements()` vira `totalElements`;
- `getTotalPages()` vira `totalPages`;
- `isLast()` vira `last`.

### Passo 2 — atualizar o controller

O controller foi alterado de `CatalogResponse` para `PageResponseDTO`:

```java
import com.agendamento.smart.dtos.PageResponseDTO;

@GetMapping
public PageResponseDTO<ServicePackageResponse> findAll(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) UUID unitId,
        @RequestParam(required = false) UUID serviceId,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Integer totalSessions,
        @RequestParam(required = false) String name,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    return service.findAll(
            user, unitId, serviceId, active, totalSessions, name, page, size);
}
```

A forma antiga de chamar sem página continua válida. O Spring injeta `0` e
`10` quando os parâmetros são omitidos.

### Passo 3 — validar os limites

A validação fica no serviço e não somente no controller. Isso protege também
eventuais chamadas internas:

```java
private void validatePagination(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
        throw new DomainException(
                "PACKAGE_PAGINATION_ERROR",
                "Os parâmetros page e size são inválidos.",
                HttpStatus.BAD_REQUEST);
    }
}
```

Regras:

- `page=-1`: HTTP 400;
- `size=0`: HTTP 400;
- `size=101`: HTTP 400;
- `page` ausente: `0`;
- `size` ausente: `10`.

Resposta de erro:

```json
{
  "code": "PACKAGE_PAGINATION_ERROR",
  "message": "Os parâmetros page e size são inválidos.",
  "fieldErrors": {}
}
```

### Passo 4 — manter o escopo de segurança

A autorização continua acontecendo antes da consulta paginada:

```java
if (user.getRole() != UserRole.ADMIN) {
    UUID allowedUnitId = unitScopeService.requireSingleAllowedUnitId(user);

    if (unitId != null && !unitId.equals(allowedUnitId)) {
        throw DomainException.tenantResourceNotFound();
    }

    unitId = allowedUnitId;
    active = true;
} else if (unitId != null) {
    unitRepository.findByIdAndClinicId(
            unitId,
            user.getClinic().getId())
            .orElseThrow(this::packageNotFound);
}
```

Isso garante:

- a clínica sempre vem de `user.getClinic()`;
- ADMIN não atravessa a própria clínica;
- unidade informada por ADMIN precisa pertencer à clínica;
- BASIC não escolhe uma unidade fora da sessão;
- BASIC não consegue pedir pacotes inativos;
- a contagem da página já nasce restrita ao escopo permitido.

Filtrar depois da paginação seria incorreto e poderia causar vazamento de
metadados ou registros. Por isso o escopo é resolvido antes do repositório.

### Passo 5 — mover filtros para a consulta JPA

O repositório antigo foi substituído por uma consulta paginada:

```java
@Query("""
        select item from ServicePackageDefinition item
        where item.clinic.id = :clinicId
          and (:unitId is null or item.unit.id = :unitId)
          and (:serviceId is null or item.service.id = :serviceId)
          and (:active is null or item.active = :active)
          and (:totalSessions is null or item.totalSessions = :totalSessions)
          and (coalesce(:name, '') = '' or
               lower(item.name) like lower(concat('%', coalesce(:name, ''), '%')))
        """)
Page<ServicePackageDefinition> findPageByClinicIdAndFilters(
        @Param("clinicId") UUID clinicId,
        @Param("unitId") UUID unitId,
        @Param("serviceId") UUID serviceId,
        @Param("active") Boolean active,
        @Param("totalSessions") Integer totalSessions,
        @Param("name") String name,
        Pageable pageable);
```

Cada filtro é opcional. Quando o valor é nulo, a condição não restringe a
consulta. Quando existe, o banco aplica a condição antes de calcular o total.

O nome é normalizado no serviço:

```java
String normalizedName = name == null || name.isBlank()
        ? null
        : name.trim();
```

A comparação é parcial e sem diferenciação de maiúsculas/minúsculas.

#### Por que usar `coalesce` no nome?

Na primeira execução da integração, a query com `:name is null` e `lower()`
produziu no PostgreSQL um erro de inferência de tipo durante a query de
contagem:

```text
ERROR: function lower(bytea) does not exist
```

A correção foi usar:

```sql
coalesce(:name, '') = ''
```

O parâmetro passa a ser tratado como texto mesmo quando está ausente. A mesma
expressão é utilizada no `like`, mantendo a contagem e a consulta de conteúdo
coerentes.

### Passo 6 — criar o `PageRequest`

O serviço cria a paginação e a ordenação:

```java
Page<ServicePackageDefinition> packages =
        packageRepository.findPageByClinicIdAndFilters(
                user.getClinic().getId(),
                unitId,
                serviceId,
                active,
                totalSessions,
                normalizedName,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.ASC, "name")));
```

O banco recebe `offset`/`limit` equivalentes ao `page` e `size`, ordenados por
nome. A página é mapeada sem trazer o restante da coleção:

```java
return PageResponseDTO.from(packages.map(this::toResponse));
```

### Passo 7 — manter mutations e detalhe

Os endpoints abaixo não foram alterados em sua semântica:

```text
POST  /api/service-packages
GET   /api/service-packages/{id}
PATCH /api/service-packages/{id}
PATCH /api/service-packages/{id}/status
```

Continuam valendo:

- `ROLE_ADMIN` para gestão;
- clínica derivada do usuário;
- validação de unidade e serviço na clínica;
- normalização de descrição e preço;
- rejeição de dias duplicados;
- erro `PACKAGE_IMMUTABLE` quando há agendamentos vinculados;
- ausência de DELETE físico.

## 6. Implementação correspondente no frontend

### 6.1 Antes: lista simples

O adaptador antigo descartava os metadados:

```ts
function contentOf(value: unknown): ServicePackage[] {
  if (Array.isArray(value)) return value as ServicePackage[]
  return (value as { content?: ServicePackage[] }).content ?? []
}

getServicePackages: builder.query<ServicePackage[], ServicePackageFilters | void>({
  query: (params) => ({ url: '/api/service-packages', params }),
  transformResponse: contentOf,
})
```

### 6.2 Depois: página normalizada

O frontend envia `page`, `size` e filtros e aceita o envelope novo:

```ts
export interface ServicePackageListQuery extends ServicePackageFilters {
  page: number
  size: number
}

export interface ServicePackagePage {
  content: ServicePackage[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
```

O normalizador preserva a resposta real e trata legado como uma única página:

```ts
export function normalizeServicePackagePage(
  value: unknown,
  fallback: PageFallback = { page: 0, size: 10 },
): ServicePackagePage {
  const record = asRecord(value)
  const content = Array.isArray(value)
    ? (value as ServicePackage[])
    : Array.isArray(record?.content)
      ? (record.content as ServicePackage[])
      : []

  const hasPaginationMetadata =
    record &&
    typeof record.page === 'number' &&
    typeof record.size === 'number' &&
    typeof record.totalElements === 'number' &&
    typeof record.totalPages === 'number' &&
    typeof record.last === 'boolean'

  if (hasPaginationMetadata) {
    return {
      content,
      page: numberOr(record.page, fallback.page),
      size: numberOr(record.size, fallback.size),
      totalElements: numberOr(record.totalElements, content.length),
      totalPages: numberOr(record.totalPages, content.length ? 1 : 0),
      last: Boolean(record.last),
    }
  }

  return {
    content,
    page: 0,
    size: fallback.size,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    last: true,
  }
}
```

### 6.3 Componentes novos

O `PackagesPage` passou a orquestrar estes componentes:

| Componente | Responsabilidade |
|---|---|
| `PackageTableFilter` | rascunho, aplicação e limpeza de filtros |
| `PackageTable` | tabela mestre, seleção e ações |
| `PackagePagination` | navegação e metadados |
| `PackageDetailPanel` | detalhe contextual |
| `PackageFormModal` | criação e edição |

Estado principal da página:

```tsx
const [filters, setFilters] = useState<ServicePackageFilters>({})
const [page, setPage] = useState(0)
const [selectedId, setSelectedId] = useState<string | null>(null)
const [editingId, setEditingId] = useState<string | null>(null)
const [modalOpen, setModalOpen] = useState(false)

const packages = useGetServicePackagesQuery(
  { ...filters, page, size: 10 },
  { skip: sessionQuery.isLoading || !isAdmin },
)

const items = packages.data?.content ?? []
```

Aplicar ou limpar filtros sempre volta para a primeira página:

```tsx
function applyFilters(nextFilters: ServicePackageFilters) {
  setFilters(nextFilters)
  setPage(0)
  setSelectedId(null)
}

function clearFilters() {
  setFilters({})
  setPage(0)
  setSelectedId(null)
}
```

O detalhe usa o item da tabela imediatamente e consulta o endpoint de detalhe
quando há seleção:

```tsx
const focusedId = editingId ?? selectedId
const detailQuery = useGetServicePackageQuery(focusedId ?? '', {
  skip: !focusedId,
})

const selectedListItem = items.find((item) => item.id === selectedId) ?? null
const selectedItem = selectedId
  ? detailQuery.data ?? selectedListItem
  : null
```

Em telas largas o grid exibe mestre e detalhe lado a lado; em telas menores o
fluxo é empilhado:

```tsx
<div
  className={
    selectedItem
      ? 'grid gap-6 xl:grid-cols-[minmax(0,1.65fr)_minmax(320px,0.75fr)]'
      : undefined
  }
>
  <section aria-label="Lista de pacotes">
    <PackageTable ... />
    <PackagePagination ... />
  </section>
  {selectedItem ? <PackageDetailPanel ... /> : null}
</div>
```

### 6.4 Formulário modal

O formulário original foi extraído sem alterar o schema ou o payload. O modal
recebe `isEditing` e `initialValues`:

```tsx
function formFromPackage(item: ServicePackage): PackageFormValues {
  return {
    name: item.name,
    description: item.description ?? '',
    unitId: item.unitId,
    serviceId: item.serviceId,
    totalSessions: String(item.totalSessions),
    totalPrice: item.totalPrice,
    weekDays: item.weekDays,
  }
}
```

O schema continua sendo a autoridade de validação da interface:

```tsx
const parsed = packageFormSchema.safeParse(form)

if (!parsed.success) {
  const errors = parsed.error.flatten().fieldErrors
  setFieldErrors(/* primeira mensagem por campo */)
  return
}

onSubmit(parsed.data)
```

O modal usa o primitive de diálogo já existente para foco contido e retorno de
foco:

```tsx
function openCreateModal(event: MouseEvent<HTMLButtonElement>) {
  modalReturnFocus.current = event.currentTarget
  setEditingId(null)
  setModalOpen(true)
}

function handleModalOpenChange(open: boolean) {
  setModalOpen(open)
  if (!open) {
    setEditingId(null)
    requestAnimationFrame(() => modalReturnFocus.current?.focus())
  }
}
```

### 6.5 Ajuste do diálogo de agendamento

Como a lista deixou de ser diretamente um array, o consumidor de agendamento
foi ajustado.

Antes:

```tsx
const packagesQuery = useGetServicePackagesQuery({
  active: true,
  unitId: effectiveUnitId || undefined,
})

const selectedPackage = packagesQuery.data?.find(
  (item) => item.id === packageId,
)
```

Depois:

```tsx
const packagesQuery = useGetServicePackagesQuery({
  active: true,
  unitId: effectiveUnitId || undefined,
  page: 0,
  size: 100,
})

const packageItems = packagesQuery.data?.content ?? []
const selectedPackage = packageItems.find(
  (item) => item.id === packageId,
)
```

O limite 100 coincide com o máximo aceito pela API. O diálogo consulta apenas
depois que a unidade efetiva está definida.

## 7. Regras de segurança preservadas

### ADMIN

- pode consultar a listagem da própria clínica;
- pode filtrar por uma unidade da própria clínica;
- recebe erro de escopo se a unidade não pertence à clínica;
- pode criar, editar e alterar situação conforme as regras existentes.

### BASIC

- pode consultar apenas a unidade autorizada na sessão;
- não consegue substituir a unidade por outra via query string;
- recebe somente pacotes ativos;
- unidade externa retorna `TENANT_RESOURCE_NOT_FOUND`/404;
- não recebe o formulário de gestão no frontend;
- continua sem permissão para mutations administrativas.

O frontend apenas esconde ou apresenta controles. A autorização efetiva é do
backend.

## 8. Testes

### 8.1 Frontend

Foram adicionados testes do normalizador para:

- resposta paginada;
- envelope `{ content }` legado;
- array legado;
- coleção vazia.

Os testes da página verificam:

- tabela e ausência de cards;
- navegação entre páginas;
- envio de `page`, `size` e filtros;
- retorno para a página zero;
- modal de criação;
- foco contido e retorno de foco;
- validação local;
- seleção mestre;
- detalhe e edição;
- fechamento do detalhe;
- ativação/desativação mantendo a linha;
- bloqueio de usuário não administrativo.

### 8.2 Backend unitário

`ServicePackageServiceTest` passou a verificar:

```java
assertThatThrownBy(() -> service.findAll(
        admin, null, null, null, null, null, -1, 10))
        .isInstanceOfSatisfying(DomainException.class, exception -> {
            assertThat(exception.getCode())
                    .isEqualTo("PACKAGE_PAGINATION_ERROR");
            assertThat(exception.getStatus().value()).isEqualTo(400);
        });
```

Também verifica que o repositório não é chamado em caso inválido e que BASIC
recebe a unidade autorizada e `active=true`:

```java
verify(packageRepository).findPageByClinicIdAndFilters(
        eq(clinic.getId()),
        eq(ownUnit.getId()),
        isNull(),
        eq(true),
        isNull(),
        isNull(),
        any());
```

### 8.3 Backend integração

`ServicePackageControllerIntegrationTest` cobre:

- primeira página;
- última página;
- nome, situação, unidade, serviço e sessões;
- resposta vazia;
- `totalElements`, `totalPages` e `last`;
- página negativa;
- tamanho acima de 100;
- BASIC e unidade externa;
- clínica externa no detalhe;
- regressão de criação, edição e ativação/desativação.

Exemplo de asserção da primeira página:

```java
mockMvc.perform(get("/api/service-packages")
                .header("Authorization", bearer(context.admin()))
                .param("page", "0")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.last").value(false));
```

## 9. Sequência recomendada de implementação

1. Ler a feature atual e identificar o contrato antigo `{ content }`.
2. Confirmar que `PageResponseDTO` já existe no backend.
3. Definir `ServicePackagePage` e `ServicePackageListQuery` no frontend.
4. Criar o normalizador compatível com as três respostas possíveis.
5. Alterar o repositório backend para `Page` e filtros JPA.
6. Alterar serviço e controller para `page`/`size`.
7. Validar limites antes de consultar o banco.
8. Preservar regras ADMIN/BASIC antes da query.
9. Extrair o filtro para `PackageTableFilter`.
10. Extrair a tabela para `PackageTable`.
11. Extrair a paginação para `PackagePagination`.
12. Extrair o detalhe para `PackageDetailPanel`.
13. Extrair o formulário para `PackageFormModal`.
14. Reescrever `PackagesPage` como orquestrador.
15. Atualizar consumidores que esperavam um array.
16. Atualizar testes e READMEs.
17. Rodar testes focados.
18. Rodar suíte completa, lint/build e validação OpenSpec.

## 10. Arquivos alterados

### Frontend

- `src/features/packages/api/packages.ts`
- `src/features/packages/api/packages.test.ts`
- `src/features/packages/pages/packages-page.tsx`
- `src/features/packages/pages/packages-page.test.tsx`
- `src/features/packages/components/package-table-filter.tsx`
- `src/features/packages/components/package-table.tsx`
- `src/features/packages/components/package-pagination.tsx`
- `src/features/packages/components/package-detail-panel.tsx`
- `src/features/packages/components/package-form-modal.tsx`
- `src/features/packages/components/package-appointment-dialog.tsx`
- `src/features/packages/README.md`

### Backend

- `src/main/java/com/agendamento/smart/controller/servicepackage/ServicePackageController.java`
- `src/main/java/com/agendamento/smart/service/ServicePackageService.java`
- `src/main/java/com/agendamento/smart/repository/ServicePackageRepository.java`
- `src/main/java/com/agendamento/smart/controller/servicepackage/README.md`
- `src/test/java/com/agendamento/smart/service/ServicePackageServiceTest.java`
- `src/test/java/com/agendamento/smart/controller/servicepackage/ServicePackageControllerIntegrationTest.java`

### OpenSpec

- Frontend: `openspec/changes/refatorar-listagem-pacotes-mestre-detalhe/`;
- Backend: `openspec/changes/paginar-listagem-pacotes/`.

## 11. Verificações executadas

### Frontend

Na aplicação da change foram registrados:

- 25 arquivos de teste e 172 testes aprovados;
- `npm run lint` aprovado;
- `npm run build` aprovado;
- validação OpenSpec estrita aprovada.

### Backend

Comandos executados:

```bash
./mvnw -Dtest=ServicePackageServiceTest,ServicePackageControllerIntegrationTest test
./mvnw verify
openspec validate paginar-listagem-pacotes --strict
```

Resultados:

- testes focados: 8 aprovados;
- suíte completa: 71 testes aprovados;
- empacotamento Spring Boot: aprovado;
- validação estrita da change: aprovada.

## 12. Rollback

Não houve migration, alteração estrutural ou dado exclusivo desta change.
Assim, o rollback é reversível por código:

1. restaurar o adaptador frontend para lista;
2. restaurar `PackagesPage` inline;
3. restaurar o retorno `CatalogResponse`;
4. restaurar a consulta de lista do repositório;
5. manter os endpoints de mutation e o modelo persistente.

O fallback do frontend também permite uma transição gradual caso algum
ambiente ainda responda apenas `{ content }`. Ele deve ser removido somente
quando todos os ambientes estiverem confirmados com o contrato paginado.

## 13. Estado final

A API entrega paginação real, filtros server-side, ordenação por nome e
metadados completos. O frontend consome esses metadados para renderizar a
tabela mestre e a navegação sem carregar toda a coleção.

As regras de domínio continuam centralizadas no backend. A interface oferece
feedback, acessibilidade e organização visual, mas não substitui autorização,
isolamento de clínica, validação ou imutabilidade.

Os arquivos de código listados neste documento e as changes OpenSpec são as
fontes de verdade para manutenção futura.

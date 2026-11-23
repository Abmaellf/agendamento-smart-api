# Design

## Context

O backend hoje usa JWT stateless lido do cabeçalho `Authorization` ou do cookie `jwt`, grava o cookie por uma hora e gera token por duas horas. Não há endpoint de logout, armazenamento de sessão ou rotação de refresh. A aplicação usa PostgreSQL e Flyway, e o filtro de segurança é o ponto comum de autenticação dos controllers.

## Goals / Non-Goals

**Goals:**

- Separar access token de refresh token sem expor segredo no JSON.
- Permitir renovação após expiração do access token sem pedir credenciais novamente.
- Revogar a sessão no logout e detectar reutilização de refresh token rotacionado.
- Preservar `Authorization` para consumidores técnicos que ainda usem access token, sem aceitar refresh token como credencial de recurso.
- Manter cookies com atributos configuráveis por ambiente e compatíveis com CORS com credenciais.

**Non-Goals:**

- Recuperação de senha, MFA, lista de dispositivos ou logout global.
- Revogar todas as sessões do usuário ao trocar senha.
- Alterar as regras de clínica, unidade, papel ou autorização de domínio.

## Decisions

### Sessão persistida e token opaco

Criar uma migration para uma sessão de autenticação e seus tokens de refresh, vinculados ao usuário. Cada valor de refresh será aleatório, terá somente seu hash armazenado e possuirá estado de uso/revogação e expiração. A sessão terá identificador estável para ligar o access JWT à revogação.

Alternativa descartada: refresh JWT stateless. Ele simplifica a emissão, mas não permite revogar logout nem detectar de forma confiável a reutilização de um token rotacionado.

### Cookies separados

Manter `jwt` como cookie de access e adicionar `refresh_token` como cookie opaco. Ambos serão `HttpOnly`, `Path=/`, com `Secure` e `SameSite` configuráveis; os tempos de vida serão configuráveis, com access curto e refresh longo. O corpo de login manterá somente dados públicos do usuário/sessão.

Alternativa descartada: enviar refresh token no corpo para o frontend guardar. Isso ampliaria o impacto de XSS e criaria uma segunda estratégia de armazenamento incompatível com o uso atual de cookies.

### Claims e validação de acesso

O access JWT incluirá o identificador da sessão e uma indicação de que é access token. O filtro continuará aceitando `Authorization` e `jwt`, preservará a precedência do cookie observada e validará assinatura, emissor, expiração e sessão não revogada antes de carregar o usuário. O refresh cookie será aceito somente pelos endpoints de ciclo de sessão.

### Rotação e replay

`POST /auth/refresh` fará busca pelo hash, verificará expiração e estado de uso, marcará o token corrente como rotacionado e criará outro token para a mesma sessão. Se um token já usado for reapresentado, toda a sessão será revogada e os cookies serão limpos. `POST /auth/logout` será público e idempotente para também funcionar quando o access JWT já expirou.

### Configuração e resposta

Adicionar propriedades explícitas para duração do access token, duração do refresh, nome/atributos dos cookies e segredo JWT, com defaults seguros para desenvolvimento e overrides do profile de produção. Respostas e logs devem usar DTOs seguros e nunca serializar a entidade `User` ou qualquer token bruto.

### CSRF e origem

Os endpoints que alteram o ciclo de cookie deverão aceitar somente requests de origens CORS configuradas e payload JSON, mantendo a lista de origens restrita aos frontends suportados. A proteção CSRF completa de todos os recursos autenticados fica fora desta change e deve ser reavaliada caso a API passe a aceitar mais origens ou formulários cross-site.

## Risks / Trade-offs

- [Risco] Consultar a sessão persistida em requests protegidos aumenta latência e carga no banco. → Mitigar com índice por identificador da sessão, consultas mínimas e medição; cache de autorização fica fora desta change.
- [Risco] Migração de cookie e resposta quebra clientes que dependem do token no JSON. → Mitigar com rollout coordenado e documentação do contrato; não manter duas estratégias de refresh no frontend.
- [Risco] Perda de refresh por limpeza do navegador encerra a sessão. → Mitigar mantendo mensagem de sessão expirada e retorno claro ao login.
- [Risco] Falha de banco durante refresh pode deixar o cliente sem renovação. → Mitigar tratando erro como falha transitória no cliente e garantindo que logout continue idempotente quando a sessão já estiver ausente.

## Migration Plan

1. Adicionar a migration e os componentes de persistência sem modificar dados existentes.
2. Publicar backend com `/auth/refresh` e `/auth/logout`, configuração de cookies e testes de integração.
3. Publicar frontend compatível com os dois endpoints e com o login sem token no corpo.
4. Invalidar cookies antigos no próximo login ou logout; sessões anteriores que não tenham registro novo devem exigir novo login.
5. Em rollback, reverter backend e frontend juntos; a migration permanece compatível por não remover tabelas existentes, mas sessões novas não devem ser interpretadas pela versão antiga.

# estoque-api

Monólito de controle de estoque e pedidos. Java 25, Spring Boot 4.1, arquitetura hexagonal, PostgreSQL e Keycloak.
As decisões estão nos ADRs do projeto: **ADR-001** (PostgreSQL), **ADR-002** (hexagonal + Java 25 + Boot 4.1) e **ADR-003** (VPS + Docker Compose).

## Estrutura

```
com.daniel.estoque
├── produto  ─┐
├── pedido    ├── domain/        regras de negócio, Java puro (+ JSpecify)
├── estoque  ─┘   application/   port/in (casos de uso), port/out (repositórios), service
│                 adapter/       in/web (controllers), out/persistence (JPA)
├── shared       exceções de domínio, auditoria JPA, tratador de erros HTTP
└── config       segurança (Keycloak) e auditoria
```

O `ArquiteturaTest` (ArchUnit) quebra o build se o domínio importar Spring, JPA ou Jackson, ou se a camada de aplicação depender de um adaptador.

## Rodando localmente

Pré-requisitos: JDK 25, Maven 3.9+ e Docker.

```bash
# 1. Postgres (5432) + Keycloak (8081) com o realm "estoque" já importado
docker compose -f compose.dev.yaml up -d

# 2. API (porta 8080). O Flyway cria as tabelas na primeira subida.
mvn spring-boot:run
```

Usuários de desenvolvimento (definidos em `docker/keycloak/realm-estoque-dev.json`):

| Usuário  | Senha       | Roles           |
|----------|-------------|-----------------|
| admin    | admin123    | admin, operador |
| operador | operador123 | operador        |

```bash
# Token (password grant: SÓ em dev)
TOKEN=$(curl -s -X POST http://localhost:8081/realms/estoque/protocol/openid-connect/token \
  -d grant_type=password -d client_id=estoque-dev -d username=admin -d password=admin123 | jq -r .access_token)

# Cadastrar produto (ADMIN)
curl -s -X POST localhost:8080/api/v1/produtos -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"nome":"Teclado","preco":150.00}'

# Dar entrada no estoque (ADMIN)
curl -s -X POST localhost:8080/api/v1/produtos/1/ajustes -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"tipo":"ENTRADA","motivo":"REPOSICAO","quantidade":10,"observacao":"Compra inicial"}'

# Criar e confirmar pedido (OPERADOR ou ADMIN)
curl -s -X POST localhost:8080/api/v1/pedidos -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"itens":[{"produtoId":1,"quantidade":3}]}'
curl -s -X POST localhost:8080/api/v1/pedidos/1/confirmar -H "Authorization: Bearer $TOKEN"

# Histórico
curl -s localhost:8080/api/v1/produtos/1/movimentacoes -H "Authorization: Bearer $TOKEN"
```

## Endpoints

Documentação interativa (OpenAPI 3 / Swagger UI), pública e sem token:

- Swagger UI: http://localhost:8080/swagger-ui.html. Gere o token como no exemplo acima e cole em **Authorize**.
- Contrato JSON: http://localhost:8080/v3/api-docs (importável no Postman/Insomnia)
- Para desligar (ex.: produção): `SWAGGER_ENABLED=false`

| Método | Caminho | Role | O que faz |
|---|---|---|---|
| POST | `/api/v1/produtos` | ADMIN | Cadastra produto (estoque inicial 0, código de barras GTIN e estoque mínimo opcionais) |
| PUT | `/api/v1/produtos/{id}` | ADMIN | Atualiza nome, descrição, preço, código de barras e estoque mínimo (não altera o saldo) |
| GET | `/api/v1/produtos`, `/api/v1/produtos/{id}` | ADMIN, OPERADOR | Consulta. A lista aceita `busca` (parte do nome ou código de barras) e `situacao` (`SALDO_BAIXO`, `SEM_ESTOQUE`, `NORMAL`) |
| POST | `/api/v1/produtos/{id}/ajustes` | ADMIN | Entrada/saída manual (REPOSICAO, AJUSTE, PERDA, INVENTARIO) |
| GET | `/api/v1/produtos/{id}/movimentacoes` | ADMIN, OPERADOR | Histórico de movimentações do produto |
| GET | `/api/v1/movimentacoes` | ADMIN, OPERADOR | Histórico de todos os produtos. Filtros: `produtoId`, `tipo`, `de`, `ate` (dias, fuso de São Paulo) |
| GET | `/api/v1/movimentacoes/totais` | ADMIN, OPERADOR | Entradas e saídas somadas no período (padrão: hoje) |
| POST | `/api/v1/pedidos` | ADMIN, OPERADOR | Cria pedido ABERTO (preço congelado no item) |
| GET | `/api/v1/pedidos` | ADMIN, OPERADOR | Lista paginada, filtro `status`. OPERADOR vê só os próprios |
| GET | `/api/v1/pedidos/{id}` | ADMIN, OPERADOR | Consulta |
| POST | `/api/v1/pedidos/{id}/confirmar` | ADMIN, OPERADOR | Baixa o estoque + gera SAIDAs |
| POST | `/api/v1/pedidos/{id}/cancelar` | ADMIN, OPERADOR | Cancela; se confirmado, devolve o estoque |

As listagens (`GET /api/v1/produtos` e `GET /api/v1/produtos/{id}/movimentacoes`) são paginadas:

- `pagina`: começa em 0 (padrão 0)
- `tamanho`: itens por página (padrão 10, máximo 100)
- `ordenacao`: `campo,asc|desc`, pode repetir. Produtos: `nome` (padrão), `preco`, `qtEstoque`. Movimentações: `id` (padrão `id,desc`), `criadoEm`. Campo fora da lista → 400.

Exemplo: `GET /api/v1/produtos?pagina=1&tamanho=20&ordenacao=preco,desc`

```json
{ "conteudo": [ ... ], "pagina": 1, "tamanho": 20, "totalElementos": 57, "totalPaginas": 3 }
```

Erros seguem o formato ProblemDetail (RFC 9457): 400 (parâmetro inválido, ex.: ordenação não permitida), 404 (não encontrado), 422 (regra de negócio, ex.: estoque insuficiente), 409 (conflito de concorrência; o cliente pode repetir).

## Garantias de estoque

1. **Domínio:** `Produto.baixar()` recusa saldo negativo.
2. **Concorrência:** `@Version` em produto (lock otimista). Duas confirmações simultâneas no mesmo produto resultam em 409 para uma delas, nunca em saldo errado. Os itens são processados em ordem de produto para evitar deadlock.
3. **Banco:** `CHECK (qt_estoque >= 0)` e `ck_mov_saldo` (saldo posterior = anterior ± quantidade) como última barreira.
4. **Histórico:** saldo e movimentação mudam na mesma transação; movimentações são `@Immutable`.

## Testes

```bash
mvn verify   # testes de domínio (JUnit puro) + ArchUnit + contrato OpenAPI (@WebMvcTest, sem banco)
```

## Deploy (VPS)

Ver ADR-003. Resumo:

```bash
# Na VPS, uma vez
cp .env.example .env && chmod 600 .env   # preencha domínio, senhas, tag
docker compose up -d

# A cada versão: crie a tag vX.Y.Z no GitHub, o CI publica a imagem no GHCR. Depois, na VPS:
sed -i 's/^API_TAG=.*/API_TAG=v0.2.0/' .env && docker compose pull api && docker compose up -d api
```

Em produção não existe realm pré-importado: crie o realm `estoque`, as roles `admin`/`operador` e um client para o front pelo console do Keycloak. Depois, troque o admin temporário por um definitivo.

### Segurança em produção

- **Console do Keycloak não é público**: o Caddy devolve 404 para `/admin` e `/realms/master`. Para administrar, use um túnel: `ssh -L 8080:<ip-do-container-keycloak>:8080 usuario@vps` e abra `http://localhost:8080/admin`.
- **Audiência do token**: a API só aceita tokens com `estoque-api` no claim `aud`. No realm `estoque`, crie um client scope com mapper *Audience* (Included Custom Audience = `estoque-api`) e associe ao client do front. Sem isso, todas as chamadas dão 401.
- **Força bruta**: em *Realm settings → Security defenses → Brute force detection*, ative a detecção (o realm de dev já vem com ela ligada).
- **Usuários do banco**: o script `docker/postgres/init` cria `estoque_migracao` (dono do schema, usado só pelo Flyway) e `estoque_app` (só DML, usado pela API). O superusuário `estoque` fica só para administração e backup.
  - Ele só roda na **primeira** subida do volume. Se o volume `pgdata` já existe, rode o SQL desse script manualmente (`docker compose exec postgres psql -U estoque`) e depois transfira as tabelas existentes no database `estoque` (o `REASSIGN OWNED` não funciona aqui, porque `estoque` é o superusuário de bootstrap):
    ```sql
    DO $$ DECLARE t text; BEGIN
      FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' LOOP
        EXECUTE format('ALTER TABLE public.%I OWNER TO estoque_migracao', t);
      END LOOP; END $$;
    GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO estoque_app;
    ```
- **Dono do pedido**: OPERADOR só vê, confirma e cancela os pedidos que criou (`criado_por` = `sub` do token); ADMIN vê e altera qualquer pedido. Pedido de outro usuário responde **404** (não 403), para não revelar quais ids existem. Pedidos antigos sem `criado_por` ficam acessíveis só ao ADMIN.
- **Limite de requisições**: 300 por minuto por usuário (`LIMITE_REQUISICOES_POR_MINUTO`); acima disso, 429 com `Retry-After`. É em memória, então vale para uma instância só da API.

### Verificações de segurança

Todas gratuitas. O CI roda tudo em cada PR, em cada push e toda segunda-feira (para pegar CVEs novas):

| O quê | Local | CI |
|---|---|---|
| SAST Java (SpotBugs + FindSecBugs) | `mvn -P seguranca verify` | job `build` |
| CVEs de dependências, segredos, misconfig do Dockerfile | `trivy fs --scanners vuln,secret,misconfig --skip-dirs target --skip-files .env .` | job `seguranca` |
| SAST multi-linguagem (Java, Dockerfile, Actions, compose) | (Semgrep; no Windows pode ser bloqueado pelo Controle de Aplicativos) | job `seguranca` |
| CVEs da imagem final | `trivy image <imagem>` | job `imagem`: **bloqueia a publicação** se houver HIGH/CRITICAL com correção |
| Atualização de dependências, digests e actions | — | Dependabot (`.github/dependabot.yml`) |

- Imagens fixadas por digest (`tag@sha256:...`) e actions por SHA de commit: uma tag reescrita não muda o que roda.
- Exceções revisadas ficam em `spotbugs-excecoes.xml` e `.trivyignore` (com data de expiração). Não adicione nada lá sem justificar.
- Os overrides de Tomcat e Jackson no `pom.xml` devem sair quando o Spring Boot trouxer versões iguais ou mais novas.

## Próximos passos

- [ ] Testes de integração com Testcontainers (adaptadores JPA + teste de concorrência na confirmação)
- [ ] Sincronizar a tabela `usuario` a partir do token (nome para relatórios)
- [ ] Exclusão lógica de produto (colunas `deletado_*` já existem)
- [ ] Script de backup (`pg_dump` + cron + cópia off-site)

# Shin

Plataforma de streaming de videos baseada em microservicos, com pipeline assincrono para upload/processamento de midia, distribuicao via CloudFront e busca com OpenSearch.

## Visao geral

O repositorio concentra:

- Servicos de negocio (Java/Spring)
- Workers de processamento (Go)
- Lambdas para tarefas assincronas em AWS
- Frontend Angular
- Infraestrutura como codigo com Terraform
- Scripts de bootstrap, limpeza e teardown

Processos principais da plataforma:

- Upload simples e chunked de videos
- Encoding assincrono e notificacao de progresso
- Geracao de thumbnails automaticos e custom
- Enriquecimento e persistencia de metadados
- Entrega de conteudo com cookies assinados no CloudFront
- Busca de videos indexada em OpenSearch
- Reacoes, comentarios, inscricoes e contagem de views

## Servicos do projeto

| Servico/Componente | Stack | Papel |
|---|---|---|
| `gateway` | Java 21, Spring Cloud Gateway | Entrada unica da API, roteamento, filtros, rate limit, circuit breaker |
| `eureka-server` | Java 21, Spring Cloud Netflix | Service discovery |
| `config-server` | Java 21, Spring Cloud Config | Configuracao centralizada (dev: `config-repo` local) |
| `auth` | Java 21, Spring Security, Redis | Login, refresh token, cookie de sessao |
| `user` | Java 21, Spring Web/JPA/Flyway | Usuarios e criadores |
| `upload` | Java 21, Spring Web, S3/SQS, Redis | Upload simples/chunked e publicacao de evento de upload |
| `metadata` | Java 21, Spring Web/JPA/Flyway, SQS/SNS, Redis | Dominio de videos, playlists, tags, categorias e agregacoes |
| `streaming` | Java 21, SQS, DynamoDB, CloudFront | Entrega VOD, autorizacao de playback e evento de view |
| `interaction` | Java 21, DynamoDB, SQS | Likes/dislikes e contadores |
| `subscription` | Java 21, DynamoDB, SQS | Inscricao/desinscricao de canais |
| `comment` | Java 21, DynamoDB, SNS/SQS | Comentarios e threads |
| `search` | Java 21, OpenSearch, SQS | Indexacao e busca textual |
| `encoding` | Go, AWS SDK v2, S3/SQS | Worker de transcoding em container |
| `lambdas/thumbnail` | Go, AWS Lambda, FFmpeg layer | Geracao de thumbnails |
| `lambdas/engagement` | Go, AWS Lambda, DynamoDB/SQS | Agregacao de progresso de playback para view qualificada |
| `frontend` | Angular 21, TypeScript, Tailwind v4 | Aplicacao web |
| `infra` | Terraform | Provisionamento AWS (S3/SQS/SNS/Lambda/DynamoDB/OpenSearch/CloudFront) |

## Estrutura do projeto

```text
shin/
|-- auth/
|-- comment/
|-- commons/
|-- config-repo/
|-- config-server/
|-- eureka-server/
|-- gateway/
|-- interaction/
|-- metadata/
|-- search/
|-- streaming/
|-- subscription/
|-- upload/
|-- user/
|-- encoding/             # Worker Go em container
|-- lambdas/
|   |-- thumbnail/        # Lambda de thumbnails (Go)
|   `-- engagement/       # Lambda de agregacao de progresso/view
|-- frontend/
|-- infra/
|-- parent-pom/
|-- scripts/
|-- docker-compose.yml
`-- .env
```

## Tecnologias utilizadas

- Backend: Java 21, Spring Boot 3.3.x, Spring Cloud 2023, Spring Cloud AWS, OpenFeign, Resilience4j
- Persistencia: PostgreSQL (JPA + Flyway), DynamoDB, Redis
- Mensageria/Eventos: AWS SQS (com DLQ), AWS SNS
- Midia/CDN: S3, CloudFront, cookies assinados
- Busca: OpenSearch Serverless
- Workers/Lambda: Go (encoding, thumbnail), AWS Lambda, FFmpeg layer
- Frontend: Angular 21, TypeScript 5.9, Tailwind CSS v4, Dash.js, Vitest
- Infra e operacao: Docker, Docker Compose, Terraform, AWS CLI

## Recursos AWS provisionados

Principais recursos criados pelo Terraform em `infra/`:

- Buckets S3: raw uploads, processed videos, thumbnails, creator pictures
- Filas SQS (com DLQ): pipeline de processamento, eventos sociais e notificacoes
- Topicos SNS: `raw-upload-created`, `video-published`, `thread-created`, `comment-reply`
- CloudFront: distribuicao para videos/thumbnails/creator assets
- Secrets Manager: chave privada do CloudFront para signed cookies
- DynamoDB: tabelas de reacao, inscricao, comentarios e sessoes de playback
- Lambda: `thumbnail-processor` e `engagement-processor`
- OpenSearch Serverless: collection para busca
- EventBridge/CloudTrail para pipeline de view (opcional via flag Terraform)

## Pre-requisitos

- Docker e Docker Compose
- Terraform >= 1.0
- AWS CLI configurada (`aws configure`)
- `jq`
- `openssl`, `curl`, `tar`, `zip`, `unzip` (usados pelos scripts)
- Shell `bash` ou `fish`

Para desenvolvimento fora de Docker:

- Java 21 + Maven
- Node.js 20+ e npm 10+ (frontend)
- Go 1.22+ (workers/lambdas)

## Configuracao de ambiente

1. Ajuste o arquivo `.env` na raiz (portas locais, credenciais e defaults).
2. Garanta acesso AWS valido (`aws sts get-caller-identity`).
3. Use o profile `dev` (padrao no `docker-compose.yml`).

Observacao: em dev, o `config-server` usa `native` profile e le arquivos de `config-repo/` montados no container.

## Subir ambiente dev (fluxo recomendado)

### Bash

```bash
chmod +x ./scripts/init-dev-environment.sh
./scripts/init-dev-environment.sh
```

Esse script:

- Gera chave RSA do CloudFront (se nao existir)
- Builda os pacotes Lambda (`engagement` e `thumbnail`) e layer do FFmpeg
- Executa `terraform init` + `terraform apply`
- Exporta outputs Terraform em variaveis de ambiente usadas pelos servicos
- Sobe os containers com `docker compose up -d`

Atencao: a versao Bash executa teardown previo (`docker compose down -v` + `terraform destroy`) antes de recriar o ambiente.

### Fish

```fish
chmod +x ./scripts/init-dev-environment.fish
source ./scripts/init-dev-environment.fish
```

A versao Fish aplica infraestrutura e sobe servicos, com tratamento para import de recursos AWS ja existentes (CloudFront e DynamoDB).

## Subir somente containers locais (sem reprovisionar infra)

Use esse fluxo quando a infraestrutura AWS ja estiver criada e as variaveis ja estiverem resolvidas.

```bash
docker compose up -d
```

Para acompanhar:

```bash
docker compose ps
docker compose logs -f
```

## Frontend local

O frontend nao esta no `docker-compose.yml` atual. Rode separadamente:

```bash
cd frontend
npm install
npm start
```

App local: `http://localhost:4200`

## Endpoints uteis em dev

- Eureka UI: `http://localhost:8761`
- Config Server health: `http://localhost:8888/actuator/health`
- Gateway health: `http://localhost:8080/actuator/health`
- Swagger centralizado: `http://localhost:8080/swagger-ui.html`
- Prefixo API Gateway: `http://localhost:8080/api/v1/...`

Rotas principais no gateway:

- `/api/v1/uploads/**` -> upload-service
- `/api/v1/videos/**`, `/tags/**`, `/categories/**`, `/playlists/**` -> metadata-service
- `/api/v1/vod/**` -> streaming-service
- `/api/v1/reactions/**` -> interaction-service
- `/api/v1/subscriptions/**` -> subscription-service
- `/api/v1/comments/**`, `/api/v1/threads/**` -> comment-service
- `/api/v1/search/**` -> search-service

## Processos detalhados

### 1) Upload e criacao inicial de video

- Cliente chama `upload-service` para upload simples ou chunked
- Arquivo final vai para bucket raw (`<videoId>/original.mp4`)
- `upload-service` envia evento `video-upload-created` para metadata iniciar registro do video

### 2) Encoding assincrono

- S3 ObjectCreated no raw bucket publica em SNS `raw-upload-created`
- Fanout envia para fila `decode-job`
- Worker `encoding` (Go) transcodifica e publica:
  - progresso em `encoding-progress`
  - resultado final em `encoding-finished-events`
- `metadata-service` atualiza status/resolucoes/duracao

### 3) Thumbnails automaticos e custom

- Evento de upload bruto e/ou upload custom no bucket de thumbnails alimenta filas
- Lambda `thumbnail-processor` gera variantes de thumbnail
- Publica resultado em `thumbnail-finished-events`
- `metadata-service` atualiza thumbnail do video

![image](/docs/images/Thumbnail%20upload.png)

### 4) Publicacao e indexacao de busca

- Ao publicar video, `metadata-service` envia SNS `video-published`
- Fanout para `video-published-opensearch-indexer`
- `search-service` consome evento e indexa no OpenSearch

### 5) Playback e views

- `streaming-service` valida acesso ao video
- Gera cookies assinados do CloudFront + token de playback
- Retorna o timestamp do ultimo acesso caso exista
- Aplicar regras referentes a tempo minimo para uma view ser considerada valida 
- Envia evento de view para fila `view-events`
- `metadata-service` aplica dedupe e incrementa view count

![image](/docs/images/Streaming.png)

### 6) Interacoes e social

- `interaction-service` grava reacoes em DynamoDB e publica `like-events`/`dislike-events`
- `subscription-service` grava inscricoes em DynamoDB e publica `channel-subscribed`/`channel-unsubscribed`
- `comment-service` trabalha com comments/threads em DynamoDB e publica eventos de thread/comment

## Encerrar ambiente e destruir infraestrutura

Scripts de limpeza e teardown (`dev`):

### Bash

```bash
chmod +x ./scripts/cleanup-and-destroy.sh
./scripts/cleanup-and-destroy.sh
```

### Fish

```fish
chmod +x ./scripts/cleanup-and-destroy.fish
source ./scripts/cleanup-and-destroy.fish
```

Esses scripts:

- Fazem purge de filas SQS
- Esvaziam buckets S3
- Executam `terraform destroy`
- Preservam recursos de CloudFront removendo-os do state antes do destroy

## Limpeza de dados sem destruir infraestrutura

Script util para reset funcional de ambiente:

```fish
source ./scripts/clean-up-aws-data.fish
```

Ele limpa dados de S3, SQS, DynamoDB, PostgreSQL e Redis, mantendo dados de `user.users` e `user.creators` no PostgreSQL.

## Testes e build (referencia rapida)

Backend (exemplos):

```bash
mvn -f metadata/pom.xml test
mvn -f gateway/pom.xml test
```

Frontend:

```bash
cd frontend
npm run test
npm run build
```

Workers Go:

```bash
cd encoding && go build ./...
cd lambdas/thumbnail && go build ./...
cd lambdas/engagement && go build ./...
```

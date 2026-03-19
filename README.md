# Shin

Plataforma de streaming de videos 

## Visao geral

O repositorio agrupa os servicos de negocio, componentes de infraestrutura local (Docker) e infraestrutura cloud (Terraform). O fluxo principal cobre:

- Upload de video
- Processamento/encoding assincrono
- Geracao de thumbnails
- Persistencia de metadados
- Entrega de conteudo via CloudFront

## Estrutura do projeto

```text
shin/
|-- auth/            # Autenticacao, autorizacao e sessoes
|-- commons/         # Bibliotecas compartilhadas entre servicos Java
|-- config-repo/     # Configuracoes externas consumidas pelo Config Server
|-- config-server/   # Spring Cloud Config Server
|-- eureka-server/   # Service discovery (Spring Cloud Netflix Eureka)
|-- gateway/         # API Gateway (roteamento, filtros, rate limit, resiliencia)
|-- metadata/        # Metadados de videos, playlists e categorias
|-- upload/          # Upload bruto/particionado e orquestracao de chunks
|-- user/            # Gestao de usuarios e criadores
|-- encoding/        # Worker de encoding consumindo filas SQS
|-- thumbnail/       # Worker de thumbnails consumindo filas SQS
|-- frontend/        # Aplicacao web em Angular
|-- infra/           # Provisionamento AWS com Terraform
|-- scripts/         # Scripts para bootstrap e cleanup de ambiente dev
|-- docker-compose.yml
`-- .env             # Variaveis locais para containers e servicos
```

## Tecnologias utilizadas

- **Backend**: Java 21, Spring Boot 3, Spring Cloud, Spring Cloud AWS
- **Frontend**: Angular (v21), TypeScript, Tailwind CSS
- **Dados e cache local**: PostgreSQL, Redis
- **Infraestrutura**: Docker, Docker Compose, Terraform
- **Cloud AWS**: S3, SQS, SNS, CloudFront

## Pre-requisitos

- Docker e Docker Compose
- Terraform (>= 1.0)
- AWS CLI configurada (`aws configure`)
- `jq` (usado para ler outputs do Terraform)
- Shell `bash` ou `fish`

## Subir ambiente local com infraestrutura AWS (fluxo recomendado)

Este fluxo executa Terraform para provisionar infra e depois sobe os servicos no Docker Compose.

### Bash

```bash
chmod +x ./scripts/init-dev-environment.sh
./scripts/init-dev-environment.sh
```

### Fish

```fish
chmod +x ./scripts/init-dev-environment.fish
source ./scripts/init-dev-environment.fish
```

## Subir apenas os containers locais (sem reprovisionar infra)

Se a infraestrutura ja estiver criada, voce pode apenas iniciar os servicos locais:

```bash
docker compose up -d
```

Para acompanhar status e logs:

```bash
docker compose ps
docker compose logs -f
```

## Encerrar ambiente e destruir infraestrutura

Os scripts abaixo realizam limpeza de filas/buckets e `terraform destroy` no ambiente `dev`.

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

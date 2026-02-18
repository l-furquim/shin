# shin

Plataforma de streaming de videos em tempo real inspirada no youtube.

## Estrutura do projeto

```bash
shin/
├── docker-compose.yml # Docker compose local para subir os containers locais. 
├── auth/          # Servico responsavel pela autenticacao e gerenciamento de sessoes do usuario 
├── commons/       # Projeto com utilitarios compartilhados entre os microservicos
├── config-repo/   # Template do config repo a ser utilizado pelos servicos
├── config-server/ # Servico responsavel por lidar com as requisicoes de configuracoes dos servicos
├── encoding/      # Servico responsavel por receber jobs de uma fila sqs e realizar o encoding dos videos nas resolucoes solicitadas 
├── eureka-server/ # Servico responsavel pelo service discovery e registration  
├── frontend # Client web construido com AngularJS
├── gateway # API Gateway resposavel pelo roteamento, rate limiting, auth filter, circuit breaker e gerenciamento de sessoes  
├── infra # Infraestrutura AWS provisionada pelo terraform
├── metadata # Servico responsavel por lidar com o metadata de videos, playlists e categorias. 
├── scripts # Scripts utilitarios para configuracao de ambientes 
├── thumbnail # Servico responsavel por receber jobs de uma fila sqs e gerar thumbnail para o video 
├── upload # Servico responsavel pelo upload bruto ou particionado de videos, e gerenciamento das chunks. 
└── user # Servico responsavel pelos usuarios e criadores do sistema. 
```

## Rodando o projeto localmente

Este projeto utiliza docker e scripts para facilitar o desenvolvimento e execução local da infraestrutura e servicos da AWS, incluindo o banco de dados, servicos e cloudfront.

### Iniciando o projeto

```bash
# Se estiver utilizando bash 
chmod +x ./scripts/init-dev-environment.sh

./scripts/init-dev-environment.sh

# Se estiver utilizando fish
chmod +x ./scripts/init-dev-environment.fish

source scripts/init-dev-environment.fish
```

## Derrubando as aplicacoes e infraestrutura AWS

```bash
# Se estiver utilizando bash 
chmod +x ./scripts/cleanup-and-destroy.sh

./scripts/cleanup-and-destroy.sh

# Se estiver utilizando fish
chmod +x ./scripts/cleanup-and-destroy.fish

source scripts/cleanup-and-destroy.fish
```

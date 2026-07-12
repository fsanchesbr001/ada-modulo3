# Projeto ADA - Módulo III

Idealizadores: Alice Postai,Osiel e Fabricio Sanches

## Pré-requisitos

- Java 21
- Docker Desktop (com Docker Compose)
- Maven Wrapper (`mvnw.cmd`)

## Como subir o projeto

### 1) Subir tudo (infra + microsserviços) em ordem automática

No diretório raiz do repositório:

```powershell
docker compose -f .\compose.yaml up -d --build
```

O `compose.yaml` já define `depends_on` + `healthcheck`, então os serviços sobem respeitando a disponibilidade dos componentes de infraestrutura.

Para acompanhar status:

```powershell
docker compose -f .\compose.yaml ps
```

Para parar:

```powershell
docker compose -f .\compose.yaml down
```

## URLs de Swagger

Swagger UI (padrão Springdoc):

- API Gateway: http://localhost:8080/swagger-ui/index.html - usuario: `admin` / senha: `admin`
- MS Faturas: http://localhost:8081/swagger-ui/index.html
- MS Pagamentos: http://localhost:8082/swagger-ui/index.html
- MS Comprovantes: http://localhost:8083/swagger-ui/index.html

> `ms-notificacoes` e `ms-backoffice` não expõem API REST pública neste projeto, por isso não possuem URL de Swagger.

Contratos OpenAPI versionados no repositório:

- `.specs/openapi/api-gateway.yaml`
- `.specs/openapi/ms-faturas.yaml`
- `.specs/openapi/ms-pagamentos.yaml`
- `.specs/openapi/ms-comprovantes.yaml`

## URLs do Grafana

- Home: http://localhost:3000
- Login padrão: `admin` / `admin`
- Dashboard Gateway Auth Overview: http://localhost:3000/d/gateway-auth-overview
- Dashboard Faturas Lifecycle: http://localhost:3000/d/faturas-lifecycle
- Dashboard Pagamentos Saga: http://localhost:3000/d/pagamentos-saga
- Dashboard Comprovantes Throughput: http://localhost:3000/d/comprovantes-throughput
- Dashboard Notificações DLT: http://localhost:3000/d/notificacoes-dlt
- Dashboard Trace Correlation: http://localhost:3000/d/trace-correlation

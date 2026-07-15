# Projeto ADA - Módulo III

Idealizadores: Alice Postai,Osiel Mesquita de Oliveira e Fabricio Sanches

# Estratégia de Replicação em Cloud

Pesquisa teórica sobre a estratégia de replicação em nuvem para o ecossistema de microsserviços de pagamento de faturas via PIX (Pagamento/Fatura com SAGA, Comprovantes com RabbitMQ + Redis, Notificação com Kafka).

---

## 1. Contexto e Objetivos

Como a aplicação foi construída do zero (_cloud-native_), não existe base legada para migrar. A estratégia de replicação, portanto, não trata de migração (_lift-and-shift_), mas de como **implantar e replicar** os componentes do ecossistema em nuvem para atingir três objetivos clássicos:

1. **Alta disponibilidade (HA):** o sistema continua operando mesmo com falha de instâncias, zonas ou serviços individuais.
2. **Escalabilidade horizontal:** aumentar capacidade adicionando réplicas, e não máquinas maiores — essencial para picos de transações PIX.
3. **Durabilidade e recuperação de desastres (DR):** nenhum pagamento confirmado ou comprovante gravado pode ser perdido.

A replicação é analisada em quatro camadas: **serviços (stateless)**, **bancos de dados**, **mensageria (RabbitMQ e Kafka)** e **cache (Redis)** — pois cada camada replica de forma diferente e com garantias diferentes.

---

## 2. Fundamentos Teóricos

### 2.1 Teorema CAP e consistência

Em um sistema distribuído replicado, é impossível garantir simultaneamente Consistência forte, Disponibilidade e Tolerância a Partições (CAP). Nossa arquitetura já assume **consistência eventual** entre serviços — é exatamente por isso que usamos o padrão **SAGA** no serviço de Pagamento: a fatura só é marcada como paga após a confirmação da geração do comprovante, com transações compensatórias em caso de falha. A replicação em nuvem deve preservar essa premissa: dentro de cada microsserviço a consistência é forte (banco próprio); entre microsserviços, eventual (eventos).

### 2.2 Replicação síncrona vs. assíncrona

- **Síncrona:** a escrita só é confirmada após todas (ou um quórum) as réplicas persistirem o dado. Garante RPO ≈ 0 (nenhuma perda), ao custo de maior latência. Indicada para o banco de faturas/pagamentos.
- **Assíncrona:** a réplica recebe o dado com atraso (_replication lag_). Menor latência, mas há janela de perda em caso de falha do primário. Indicada para réplicas de leitura e replicação entre regiões.

### 2.3 Topologias

- **Multi-AZ (zonas de disponibilidade):** réplicas distribuídas em datacenters distintos da mesma região. É a base mínima de HA e o padrão recomendado para este projeto.
- **Multi-Region:** réplicas em regiões geográficas distintas, para DR de grande escala. Normalmente **ativo-passivo** (região secundária em standby com replicação assíncrona) — suficiente para o nosso cenário. **Ativo-ativo** multi-região traria complexidade de conflito de escrita desnecessária aqui.

### 2.4 Métricas de recuperação

- **RTO (Recovery Time Objective):** tempo máximo aceitável de indisponibilidade.
- **RPO (Recovery Point Objective):** perda máxima aceitável de dados. Para transações financeiras (PIX), o alvo é **RPO = 0** na camada de pagamento, o que justifica replicação síncrona nessa camada.

---

## 3. Replicação por Camada da Arquitetura

### 3.1 Microsserviços (camada stateless)

Os três serviços (Pagamento/Fatura, Comprovantes, Notificação) são **stateless**: todo estado vive no banco, no cache ou nos brokers. Isso permite a forma mais simples e poderosa de replicação:

- **Containerização (Docker)** e orquestração com **Kubernetes** (EKS/GKE/AKS) ou serviço gerenciado equivalente (ECS/Cloud Run).
- Cada serviço roda com **mínimo de 2–3 réplicas (pods)** distribuídas entre zonas de disponibilidade via _anti-affinity_.
- **Load balancer / API Gateway** na frente, distribuindo requisições entre réplicas.
- **Autoscaling horizontal (HPA):** novas réplicas são criadas automaticamente conforme CPU/memória ou métricas customizadas (ex.: profundidade da fila do RabbitMQ — _KEDA_), absorvendo picos de pagamento.
- **Health checks (liveness/readiness):** réplicas doentes são removidas do balanceamento e recriadas automaticamente (_self-healing_).

Observação importante: replicar consumidores exige **idempotência**. Como o POST de comprovantes gera UUID v4 e o processamento é assíncrono, os consumers devem tratar reentrega de mensagens (semântica _at-least-once_) sem duplicar comprovantes.

### 3.2 Bancos de dados (um por serviço — _database per service_)

Cada microsserviço mantém seu banco segregado, e cada um é replicado de forma independente usando serviços gerenciados (ex.: Amazon RDS/Aurora, Cloud SQL, Azure Database):

- **Padrão primário–réplica (leader–follower):** todas as escritas vão ao primário; réplicas recebem o log de transações (WAL/binlog).
- **Multi-AZ com failover automático:** uma réplica síncrona em outra zona assume como primário em caso de falha (RTO de segundos a poucos minutos, RPO ≈ 0).
- **Réplicas de leitura (assíncronas):** o GET de comprovantes, quando há _cache miss_ no Redis, pode ser direcionado a réplicas de leitura, aliviando o primário — combinando bem com a estratégia de cache já implementada.
- **Backups automáticos + PITR (point-in-time recovery):** snapshots diários e logs contínuos permitem restaurar o banco a qualquer instante, cobrindo erros lógicos que a replicação sozinha não cobre (replicação propaga também o erro).

### 3.3 RabbitMQ (fila de gravação de comprovantes)

- Em nuvem, usar **cluster de 3 nós** distribuídos em zonas distintas (ou o serviço gerenciado **Amazon MQ / CloudAMQP**).
- Utilizar **quorum queues**, que replicam cada fila em múltiplos nós usando o algoritmo de consenso **Raft**: a mensagem só é confirmada ao producer após ser gravada na maioria dos nós. Isso substitui as antigas _mirrored queues_ e garante que a mensagem do comprovante não se perde se um nó cair.
- **Publisher confirms + mensagens persistentes:** o serviço de Comprovantes só responde 202 - Accepted de forma segura porque o broker confirmou a persistência replicada da mensagem.
- **Consumers replicados:** múltiplas réplicas do consumer competem pela mesma fila (_competing consumers_), escalando a vazão de gravação no banco.

### 3.4 Apache Kafka (tópico de "Pagamento Realizado com Sucesso")

O Kafka é replicado por design, e sua configuração é o coração da resiliência da notificação:

- **Cluster com 3+ brokers** em zonas distintas (ou gerenciado: **Amazon MSK / Confluent Cloud**), com _rack awareness_ para espalhar réplicas entre AZs.
- **Replication factor = 3** por tópico: cada partição possui 1 líder e 2 seguidores.
- **min.insync.replicas = 2** e producer com **acks=all**: o evento de pagamento só é considerado publicado quando gravado em pelo menos 2 réplicas — tolera a queda de 1 broker sem perda.
- **Partições múltiplas + consumer groups:** as réplicas do microsserviço de Notificação formam um _consumer group_; o Kafka distribui as partições entre elas, escalando o consumo horizontalmente.
- A resiliência de consumo já implementada com **@RetryableTopic** (tópicos de retry com _backoff_ e DLT) se soma à replicação do cluster: a replicação protege contra falha de infraestrutura; o retry protege contra falha de processamento.

### 3.5 Redis (cache compartilhado de comprovantes)

- Usar serviço gerenciado (**ElastiCache / MemoryStore / Azure Cache**) em modo **replicado**: 1 primário + réplicas de leitura em AZs distintas, com failover automático (mecanismo estilo **Sentinel**).
- Para grande escala, **Redis Cluster** particiona o keyspace entre shards, cada shard com sua réplica.
- A replicação do Redis é **assíncrona** — aceitável aqui, pois o cache **não é fonte de verdade**: em caso de perda, o fluxo já previsto de _cache miss_ (buscar no banco → repovoar o Redis → retornar) reconstrói o estado naturalmente. Isso torna o cache a camada mais tolerante a perda de todo o ecossistema.
- Boas práticas complementares: **TTL** nas chaves e política de evicção adequada, evitando crescimento indefinido.

---

## 4. Visão Consolidada

| Componente           | Mecanismo de replicação                          | Tipo                                 | Objetivo principal                         |
| -------------------- | ------------------------------------------------ | ------------------------------------ | ------------------------------------------ |
| Serviços (3x)        | Réplicas de pods + HPA + load balancer           | N/A (stateless)                      | Escala e HA                                |
| Bancos (por serviço) | Primário–réplica Multi-AZ + read replicas + PITR | Síncrona (HA) / Assíncrona (leitura) | RPO ≈ 0, leitura escalável                 |
| RabbitMQ             | Quorum queues (Raft) em cluster de 3 nós         | Consenso por maioria                 | Não perder mensagens de comprovante        |
| Kafka                | Replication factor 3, acks=all, min.insync=2     | Quórum de ISR                        | Durabilidade dos eventos de pagamento      |
| Redis                | Primário–réplica com failover automático         | Assíncrona                           | Disponibilidade do cache (perda tolerável) |

---

## 5. Considerações Finais

A estratégia proposta segue o princípio de que **cada camada replica segundo a criticidade do seu dado**: replicação síncrona/por quórum onde perda é inaceitável (pagamentos, filas, eventos) e assíncrona onde o dado é reconstruível (cache, réplicas de leitura). Combinada com a natureza stateless dos serviços, a orquestração por contêineres e os mecanismos de resiliência já presentes na aplicação (SAGA, retries, @RetryableTopic, idempotência), a arquitetura alcança alta disponibilidade Multi-AZ com possibilidade de evolução para DR multi-região (ativo-passivo) sem mudanças estruturais no código — apenas na topologia de implantação.


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

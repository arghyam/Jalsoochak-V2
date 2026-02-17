# Water Management Platform

A production-ready multi-module Spring Boot microservices platform built with **Java 21**, **Spring Cloud 2023.x**, **Eureka Service Discovery**, **Spring Kafka**, and **PostgreSQL**.

---

## Architecture Overview

```
                    ┌──────────────────────┐
                    │   Service Discovery  │
                    │   (Eureka Server)    │
                    │     :8761            │
                    └──────────┬───────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                     │
┌─────────┴────────┐ ┌────────┴─────────┐ ┌────────┴─────────┐
│  Tenant Service  │ │   User Service   │ │ Anomaly Service  │
│     :8081        │ │     :8082        │ │     :8083        │
└──────────────────┘ └──────────────────┘ └──────────────────┘
          │                    │
┌─────────┴────────┐ ┌────────┴─────────┐ ┌──────────────────┐
│  Telemetry       │ │  Message Service │ │  Scheme Service  │
│  Service  :8084  │ │     :8085        │ │     :8086        │
└──────────────────┘ └──────────────────┘ └──────────────────┘

All business services register with Eureka and communicate via Kafka topics.

Infrastructure: PostgreSQL (shared_db) + Apache Kafka
```

---

## Module Descriptions

| Module                       | Port   | Description                                      |
|------------------------------|--------|--------------------------------------------------|
| `service-discovery`          | 8761   | Netflix Eureka Server for service registration   |
| `tenant-service`             | 8081   | Manages tenant information                       |
| `user-service`               | 8082   | Manages user information                         |
| `anomaly-service`            | 8083   | Detects and manages anomalies (leaks, pressure)  |
| `telemetry-service`          | 8084   | Manages sensor and meter telemetry data          |
| `message-service`            | 8085   | Manages notifications (Email, WhatsApp, Webhook) |
| `scheme-service`             | 8086   | Manages water supply schemes                     |

---

## Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **Maven 3.9+**
- **Docker & Docker Compose** (for Kafka and PostgreSQL)

---

## How to Start Eureka (Service Discovery)

```bash
# From the project root
cd water-management-platform

# Build all modules
mvn clean package -DskipTests

# Start Eureka Server
java -jar service-discovery/target/*.jar
```

Eureka Dashboard: [http://localhost:8761](http://localhost:8761)

---

## Start Kafka with Docker

```bash
# Start Kafka using Docker (KRaft mode - no Zookeeper needed)
docker run -d \
  --name kafka \
  -p 9092:9092 \
  -e KAFKA_CFG_NODE_ID=0 \
  -e KAFKA_CFG_PROCESS_ROLES=controller,broker \
  -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@localhost:9093 \
  -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  bitnami/kafka:latest
```

Or use the classic Zookeeper-based approach:

```bash
# Start Zookeeper
docker run -d --name zookeeper -p 2181:2181 confluentinc/cp-zookeeper:latest \
  -e ZOOKEEPER_CLIENT_PORT=2181

# Start Kafka
docker run -d --name kafka -p 9092:9092 \
  --link zookeeper \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest
```

---

## Start PostgreSQL with Docker

```bash
docker run -d \
  --name postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=shared_db \
  -e POSTGRES_USER=YOUR_USERNAME \
  -e POSTGRES_PASSWORD=YOUR_PASSWORD \
  postgres:16-alpine
```

> **Note:** Update `YOUR_USERNAME` and `YOUR_PASSWORD` in each service's `application.yml` with real credentials.

---

## How to Run Services

### Option 1: Run All from Maven

```bash
cd water-management-platform
mvn clean package -DskipTests

# Start each service in a separate terminal
java -jar service-discovery/target/*.jar
java -jar tenant-service/target/*.jar
java -jar user-service/target/*.jar
java -jar anomaly-service/target/*.jar
java -jar telemetry-service/target/*.jar
java -jar message-service/target/*.jar
java -jar scheme-service/target/*.jar
```

### Option 2: Run Individual Service via Maven

```bash
cd tenant-service
mvn spring-boot:run
```

### Option 3: Docker

```bash
# Build the image
cd tenant-service
docker build -t tenant-service .

# Run the container
docker run -p 8081:8081 tenant-service
```

---

## API Endpoints

### Tenant Service (`:8081`)

| Method | Endpoint          | Description            |
|--------|-------------------|------------------------|
| GET    | `/api/tenants`    | Get all tenants        |
| POST   | `/api/publish`    | Publish Kafka message  |

**Sample Response** — `GET /api/tenants`:
```json
[
  { "id": 1, "name": "Tenant A" },
  { "id": 2, "name": "Tenant B" }
]
```

### User Service (`:8082`)

| Method | Endpoint          | Description            |
|--------|-------------------|------------------------|
| GET    | `/api/users`      | Get all users          |
| POST   | `/api/publish`    | Publish Kafka message  |

**Sample Response** — `GET /api/users`:
```json
[
  { "id": 1, "name": "John Doe" },
  { "id": 2, "name": "Jane Doe" }
]
```

### Anomaly Service (`:8083`)

| Method | Endpoint          | Description            |
|--------|-------------------|------------------------|
| GET    | `/api/anomalies`  | Get all anomalies      |
| POST   | `/api/publish`    | Publish Kafka message  |

**Sample Response** — `GET /api/anomalies`:
```json
[
  { "id": 1, "type": "Leak Detection" },
  { "id": 2, "type": "Pressure Drop" }
]
```

### Telemetry Service (`:8084`)

| Method | Endpoint          | Description              |
|--------|-------------------|--------------------------|
| GET    | `/api/telemetry`  | Get all telemetry data   |
| POST   | `/api/publish`    | Publish Kafka message    |

**Sample Response** — `GET /api/telemetry`:
```json
[
  { "id": 1, "meterId": "METER-001", "readingValue": 150.5 },
  { "id": 2, "meterId": "METER-002", "readingValue": 230.8 }
]
```

### Message Service (`:8085`)

| Method | Endpoint          | Description            |
|--------|-------------------|------------------------|
| GET    | `/api/messages`   | Get all messages       |
| POST   | `/api/publish`    | Publish Kafka message  |

### Scheme Service (`:8086`)

| Method | Endpoint          | Description              |
|--------|-------------------|--------------------------|
| GET    | `/api/schemes`    | Get all schemes          |
| POST   | `/api/publish`    | Publish Kafka message    |

**Sample Response** — `GET /api/schemes`:
```json
[
  { "id": 1, "schemeName": "Rural Water Supply", "schemeCode": "RWS-001" },
  { "id": 2, "schemeName": "Urban Pipeline Network", "schemeCode": "UPN-002" }
]
```

### Kafka Publishing (All Services)

```bash
curl -X POST http://localhost:8081/api/publish \
  -H "Content-Type: text/plain" \
  -d "Hello from tenant-service"
```

Each service publishes to its own topic (`<service-name>-topic`) and listens on `common-topic`.

### Actuator Health (All Services)

```
GET http://localhost:<port>/actuator/health
```

---

## Kafka Topics

| Service                    | Produces To                          | Consumes From    |
|----------------------------|--------------------------------------|------------------|
| `tenant-service`           | `tenant-service-topic`               | `common-topic`   |
| `user-service`             | `user-service-topic`                 | `common-topic`   |
| `anomaly-service`          | `anomaly-service-topic`              | `common-topic`   |
| `telemetry-service`        | `telemetry-service-topic`            | `common-topic`   |
| `message-service`          | `message-service-topic`              | `common-topic`   |
| `scheme-service`           | `scheme-service-topic`               | `common-topic`   |

---

## Future Enhancements

- **Database Separation**: Currently all services point to a single `shared_db`. In production, each service should have its own dedicated database to follow the **Database-per-Service** pattern.
- **API Gateway**: Add Spring Cloud Gateway for centralized routing, rate limiting, and authentication.
- **Config Server**: Externalize configuration using Spring Cloud Config Server.
- **Circuit Breaker**: Add Resilience4j for fault tolerance and circuit-breaking patterns.
- **Distributed Tracing**: Integrate Micrometer Tracing with Zipkin/Jaeger.
- **Security**: Add Spring Security with OAuth2/JWT for API protection.
- **Docker Compose**: Create a `docker-compose.yml` for full-stack orchestration.

---

## Project Structure

```
water-management-platform/
├── pom.xml                          # Parent POM with BOM management
├── README.md
│
├── service-discovery/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/example/discovery/
│       │   └── ServiceDiscoveryApplication.java
│       └── resources/application.yml
│
├── tenant-service/                  # Port 8081
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/example/tenant/
│       │   ├── TenantServiceApplication.java
│       │   ├── controller/ApiController.java
│       │   ├── service/BusinessService.java
│       │   ├── repository/SampleRepository.java
│       │   ├── entity/SampleEntity.java
│       │   ├── dto/SampleDTO.java
│       │   ├── kafka/
│       │   │   ├── KafkaConfig.java
│       │   │   ├── KafkaProducer.java
│       │   │   └── KafkaConsumer.java
│       │   └── config/DataSourceConfig.java
│       └── resources/application.yml
│
├── user-service/                    # Port 8082
├── anomaly-service/                 # Port 8083
├── telemetry-service/               # Port 8084
├── message-service/                 # Port 8085
└── scheme-service/                  # Port 8086
```

# Tenant Service Observability

This service now emits logs to both console and a rolling file (`../logger/logs/backend-services.log`) and exposes Prometheus metrics at `/actuator/prometheus`.

## Local setup (Loki + Grafana + Prometheus)

1. Start tenant-service locally:

```bash
cd backend/tenant-service
mvn spring-boot:run
```

2. In a new terminal, start the observability stack:

```bash
cd backend/tenant-service/observability
docker compose up -d
```

3. Open Grafana:
   - URL: `http://localhost:3000`
   - User: `admin`
   - Password: `admin`

4. Validate ingestion:
   - Logs in Loki: query `{service="tenant-service"}` (or `{app="backend-services"}`)
   - Metrics in Prometheus: query `up{job="tenant-service"}`

## What is configured

- `logback-spring.xml`
  - Writes logs to console and `../logger/logs/backend-services.log`
  - Includes service name and trace/span IDs from MDC when present
- `application.yml`
  - Exposes `health`, `info`, and `prometheus` actuator endpoints
  - Adds Micrometer `application` tag using `spring.application.name`
- `observability/docker-compose.yml`
  - Loki for logs storage
  - Promtail to tail `../logs/*.log` and push to Loki
  - Prometheus to scrape `http://host.docker.internal:8081/actuator/prometheus`
  - Grafana with auto-provisioned Loki + Prometheus datasources

## Dev/Prod deployment direction

For dev/prod, deploy Loki + Prometheus + Grafana as shared infra and keep tenant-service configuration the same:

- Point log shipping agents (Promtail or equivalent) to centralized Loki.
- Keep `logging.file.name` set to a path readable by the node-level log agent.
- Keep `/actuator/prometheus` reachable only from internal Prometheus.
- Override labels such as environment (`dev`/`prod`) at the agent layer.

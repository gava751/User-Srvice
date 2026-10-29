# User Service (InnoBoard)

Spring Boot microservice managing users and their payment cards.

## Stack

- Java 25 (LTS)
- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Actuator)
- PostgreSQL
- Liquibase (schema migrations)
- Lombok

## Configuration

All database and server settings are provided via environment variables
(with local-run defaults):

| Variable      | Default (local)                                    | Description          |
|---------------|----------------------------------------------------|----------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/user_service`    | JDBC URL             |
| `DB_USERNAME` | `user_service`                                     | DB user              |
| `DB_PASSWORD` | — (без дефолта, обязательно)                       | DB password          |
| `SERVER_PORT` | `8081` (8080 зарезервирован под API Gateway)       | HTTP port            |
| `REDIS_HOST`  | `localhost`                                        | Redis host           |
| `REDIS_PORT`  | `6379`                                             | Redis port           |


The database schema is created and versioned exclusively by Liquibase
(`src/main/resources/db/changelog`), so Hibernate runs with
`ddl-auto: validate`.

## Health probes (Kubernetes)

- Liveness: `GET /actuator/health/liveness`
- Readiness: `GET /actuator/health/readiness`

## Run locally

```bash
./mvnw spring-boot:run
```

Requires PostgreSQL and Redis:

```bash
docker compose up -d
```

## Build

```bash
./mvnw clean package
```

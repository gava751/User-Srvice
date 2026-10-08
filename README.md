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

| Variable      | Default                                            | Description          |
|---------------|----------------------------------------------------|----------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/user_service`    | JDBC URL             |
| `DB_USERNAME` | `postgres`                                         | DB user              |
| `DB_PASSWORD` | `postgres`                                         | DB password          |
| `SERVER_PORT` | `8080`                                             | HTTP port            |

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

Requires a running PostgreSQL instance, e.g.:

```bash
docker run -d --name user-service-db \
  -e POSTGRES_DB=user_service \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:17
```

## Build

```bash
./mvnw clean package
```

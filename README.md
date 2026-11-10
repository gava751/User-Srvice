# User Service (InnoBoard)

Spring Boot microservice managing users and their payment cards.

## Stack

- Java 25 (LTS)
- Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Actuator)
- Spring Security OAuth2 Resource Server (JWT)
- PostgreSQL
- Redis (caching)
- Liquibase (schema migrations)
- Lombok

## Configuration

All database and server settings are provided via environment variables
(with local-run defaults):

| Variable                | Default (local)                                    | Description          |
|-------------------------|----------------------------------------------------|----------------------|
| `DB_URL`                | `jdbc:postgresql://localhost:5432/user_service`    | JDBC URL             |
| `DB_USERNAME`           | `user_service`                                     | DB user              |
| `DB_PASSWORD`           | — (без дефолта, обязательно)                       | DB password          |
| `SERVER_PORT`           | `8082` (8080 занят API Gateway)                    | HTTP port            |
| `REDIS_HOST`            | `localhost`                                        | Redis host           |
| `REDIS_PORT`            | `6379`                                             | Redis port           |
| `AUTH_SERVICE_URL`      | `http://localhost:8081`                            | Auth Service base URL (JWKS) |
| `INTERNAL_SERVICE_TOKEN`| — (без дефолта, обязательно)                       | Shared secret for `/internal/**` |


The database schema is created and versioned exclusively by Liquibase
(`src/main/resources/db/changelog`), so Hibernate runs with
`ddl-auto: validate`.

## Security

The service acts as an OAuth2 Resource Server. Incoming `Authorization: Bearer`
tokens are validated locally against the Auth Service JWKS endpoint
(`GET /.well-known/jwks.json`), including signature, expiry and the
`type: access` claim check.

The `role` claim is mapped to Spring Security authorities (`ROLE_ADMIN`,
`ROLE_USER`) and enforced with method-level security:

- `ADMIN` — unrestricted access to all users and cards;
- `USER` — access only to own profile and own cards; the target `user_id`
  is always taken from the token (`user_id` claim), never from the path
  or body, which prevents BOLA/IDOR.

Service-to-service calls from the Auth Service (user creation on
registration, compensation delete) go through dedicated endpoints under
`/internal/**` protected by the shared `X-Internal-Token` header.

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

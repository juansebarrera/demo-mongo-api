# AGENTS.md

## Build & test commands

```bash
# Tests (embedded Mongo via Flapdoodle — no Docker needed)
mvn test

# Compile only
mvn compile

# Package (skip tests)
mvn package -DskipTests

# Docker (app + Mongo, auto-builds from source)
docker compose up --build

# Docker (pull pre-built image from GHCR)
docker compose pull && docker compose up
```

CI runs: `mvn -B clean compile` → `mvn -B test` → `mvn -B package -DskipTests`.

No separate lint or typecheck step exists. Maven compile is the closest equivalent.

## Java version

`pom.xml` targets Java 17. CI uses JDK 21. Use JDK 21+ when running Maven locally.

## Spring Boot 4.x property renames (critical gotcha)

Spring Boot 4.x renamed Mongo properties. If Mongo connection breaks, check this first:

| Old (Boot 3.x)                    | New (Boot 4.x)                |
|------------------------------------|-------------------------------|
| `spring.data.mongodb.uri`          | `spring.mongodb.uri`          |
| Env var `MONGODB_URI`              | Env var `SPRING_MONGODB_URI`  |

Embedded Mongo Flapdoodle property is `de.flapdoodle.mongodb.embedded.version` (not the Spring one).

## Environment setup

Copy `.env.example` → `.env`. Minimum required:

```
JWT_SECRET=<any secret>
ADMIN_SEED_PASSWORD=<admin password>
```

`AdminSeeder` only runs on `dev`/`test` profiles. Docker Compose sets `SPRING_PROFILES_ACTIVE=dev`.

## Architecture

- **Framework:** Spring Boot 4.1.0 + Spring Security + JWT (JJWT 0.12.6) + Lombok
- **DB:** MongoDB via Spring Data
- **Package:** `com.example.demo_mongo_api`
- **Layers:** `controller/` → `service/` → `repository/` (standard Spring pattern)
- **DTOs:** Java `record` types in `controller/dto/` — field names serialize as-is (no `@JsonProperty`), must match frontend expectations
- **Frontend:** Vanilla SPA in `src/main/resources/static/` (HTML + CSS + JS, no build step)
- **Docs:** Swagger UI at `/swagger-ui/index.html`, OpenAPI at `/v3/api-docs`

## Security rules

- `/api/auth/**`, `/actuator/**`, static files, Swagger: public
- All other endpoints: require JWT
- `DELETE` on `/api/productos/{id}` and `/api/clientes/{id}`: require `ROLE_ADMIN`
- 401/403 return JSON (custom handlers in `SecurityConfig`)

## Validation

Messages externalized in `src/main/resources/messages.properties` using `{entity.field.validation}` pattern. Adding a new validation: add message → use in annotation → no need to touch `GlobalExceptionHandler`.

## Tests

Tests use Flapdoodle embedded Mongo (configured in `src/test/resources/application.properties`). They are self-contained — no Docker or external services required. Test credentials are dummy values in that properties file.

## Scripts

`scripts/dump-mongo.ps1` / `scripts/restore-mongo.ps1` (and `.sh` variants) for MongoDB data export/import. Requires `docker exec` against the running container.

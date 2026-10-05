# Devault

Devault collects knowledge from development tools into workspaces. The ingestion service currently
backfills GitHub repositories (commits, pull requests, issues); Jira and Confluence sources and
embedding / query services are planned.

Kotlin 2.3 · Spring Boot 4.1 · Java 21 · PostgreSQL · Redis · Kafka · Gradle (Kotlin DSL)

## Modules

| Module              | Description                                                        | Local URL                      |
|---------------------|--------------------------------------------------------------------|--------------------------------|
| `auth-service`      | Registration, login, JWT (Ed25519) issuing, JWKS endpoint          | http://localhost:8081          |
| `workspace-service` | Workspaces and their members                                       | http://localhost:8080/api/v1   |
| `ingestion-service` | Source credentials, GitHub backfill                                | http://localhost:8082/api/v1   |
| `auth-service-lib`  | JWT validation via JWKS, default `SecurityFilterChain`             | –                              |
| `common-lib`        | `ApiResponse`, global exception handler, shared OpenAPI config     | –                              |
| `build-logic`       | Gradle convention plugins (`devault.springdoc`)                    | –                              |

Architecture, dependency rules and conventions: [SYSTEM.md](SYSTEM.md).

## Requirements

- JDK 21
- Docker with Docker Compose
- Python 3 – only for generating the Insomnia collection

## Local setup

### 1. Environment files

```bash
cp .env.example .env                                            # used by docker compose
cp docker-compose.override.yml.example docker-compose.override.yml  # exposes Postgres, Redis, Kafka ports
cp .env .env.local                                              # used when running services locally
```

In `.env.local` point the services at your machine: `DB_HOST=localhost`, `REDIS_HOST=localhost`,
`JWKS_HOST=localhost`. All three files are git-ignored.

### 2. JWT keys

auth-service signs tokens with an **Ed25519** key pair, passed as Base64-encoded DER
(private key: PKCS#8, public key: X.509). Generate it with the JDK – no OpenSSL needed:

```bash
make jwt-keys
```

This fills empty `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` in `.env` and `.env.local` with the same key pair.
Keys that are already set are never overwritten – clear both values to regenerate.
`java scripts/GenerateJwtKeys.java` without arguments only prints the variables.

Also set `JWT_ACCESS_EXPIRATION` and `JWT_REFRESH_EXPIRATION` (milliseconds).

<details>
<summary>Alternative: OpenSSL 3</summary>

The macOS system `openssl` (LibreSSL) does not support Ed25519 – use OpenSSL 3 (`brew install openssl`).

```bash
openssl genpkey -algorithm ed25519 -out private.pem
openssl pkey -in private.pem -outform DER | base64          # -> JWT_PRIVATE_KEY
openssl pkey -in private.pem -pubout -outform DER | base64  # -> JWT_PUBLIC_KEY
```

On Linux use `base64 -w0` to get a single line. `private.pem` / `public.pem` are git-ignored.
</details>

### 3. Infrastructure

```bash
make dev-up      # Postgres, Redis, Kafka
```

Databases and users for each service are created by `init.sh` on the first Postgres start.

### 4. Run the services

From the IDE (with the variables from `.env.local`), or from the terminal:

```bash
set -a; . ./.env.local; set +a
./gradlew :auth-service:bootRun
```

Start `auth-service` first – the other services fetch its public key (JWKS) to validate tokens.

## API documentation

Every service exposes an OpenAPI spec and Swagger UI (disabled in the `prod` profile):

| Service           | OpenAPI                                   | Swagger UI                                   |
|-------------------|-------------------------------------------|----------------------------------------------|
| auth-service      | http://localhost:8081/v3/api-docs         | http://localhost:8081/swagger-ui.html        |
| workspace-service | http://localhost:8080/api/v1/v3/api-docs  | http://localhost:8080/api/v1/swagger-ui.html |
| ingestion-service | http://localhost:8082/api/v1/v3/api-docs  | http://localhost:8082/api/v1/swagger-ui.html |

### Insomnia

With the services running:

```bash
make insomnia
```

Then in Insomnia: **Import → File → `insomnia/devault.insomnia.json`**. You get one **Devault** collection with
all services; call **login** first – the tokens are stored automatically. Details: [insomnia/README.md](insomnia/README.md).

## Tests

```bash
set -a; . ./.env.local; set +a
./gradlew build
```

The `*ApplicationTests.contextLoads` tests start the full application context and need the local infrastructure
and variables from `.env.local`; all other tests run without them.

## Production

```bash
make prod-build   # builds the images and starts everything with the `prod` compose profile
```

Service containers run with `SPRING_PROFILES_ACTIVE=prod` (API docs disabled). Other targets: `prod-up`,
`prod-down`, `prod-logs`, `prod-restart`, `prod-pull`; for local infrastructure `dev-up`, `dev-down`,
`dev-down-volumes`, `dev-logs`, `dev-restart`.

## Deployment modes

Today Devault is fully self-hosted: one team runs every service, including its own auth-service. A hybrid mode
is planned as a later stage: the project runs a central auth-service and the frontend, and each team runs the
data services on its own server, so tokens, secrets and ingested data stay there. It is not implemented yet.
Design note: [DEPLOYMENT_MODES.md](DEPLOYMENT_MODES.md).

## License

[Apache License 2.0](LICENSE)

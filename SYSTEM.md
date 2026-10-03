# Devault – system overview

## Modules

| Module              | Type          | Port | Context path | Depends on                      |
|---------------------|---------------|------|--------------|---------------------------------|
| `auth-service`      | service       | 8081 | –            | `auth-service-lib`, `common-lib` |
| `workspace-service` | service       | 8080 | `/api/v1`    | `auth-service-lib`, `common-lib` |
| `ingestion-service` | service       | 8082 | `/api/v1`    | `auth-service-lib`, `common-lib` |
| `auth-service-lib`  | library       | –    | –            | `common-lib`                    |
| `common-lib`        | library       | –    | –            | –                               |
| `build-logic`       | Gradle build  | –    | –            | –                               |

- **auth-service-lib** – JWT validation against the auth-service JWKS, `AuthenticatedUser` principal,
  default stateless `SecurityFilterChain` (`@ConditionalOnMissingBean`, so a service can replace it –
  `auth-service` does, with its own `SecurityConfig`).
- **common-lib** – shared web code: `ApiResponse`, `GlobalExceptionHandler`, OpenAPI auto-configuration.
  Everything is wired through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- **build-logic** – included build (`pluginManagement { includeBuild("build-logic") }`) with Gradle
  convention plugins, written as precompiled script plugins in `build-logic/src/main/kotlin`.

### Dependency rules

- Services may depend on both libraries.
- `auth-service-lib` → `common-lib` (uses `ApiResponse`).
- `common-lib` must **not** depend on `auth-service-lib` – it would create a cycle. Code in `common-lib`
  that needs security types uses Spring Security API only (e.g. `@AuthenticationPrincipal`), never
  `AuthenticatedUser`.

## Conventions

### Dependency versions

Versions of Gradle plugins (Kotlin, Spring Boot, dependency management) and shared dependencies live in
the version catalog `gradle/libs.versions.toml`; modules apply plugins without versions. Keep together:
- Kotlin = the `kotlin.version` managed by Spring Boot,
- springdoc line = Spring Boot minor (springdoc 3.0.x ↔ Boot 4.0.x, 3.1.x ↔ Boot 4.1.x).

### Public (no JWT) paths

`authlib.security.public-paths` (list, default empty) – paths permitted without a token, relative to the
servlet context path. Used by the default `SecurityFilterChain` in `auth-service-lib` and by
`auth-service`'s own `SecurityConfig`.

### Spring profiles

`prod` is activated by `SPRING_PROFILES_ACTIVE=prod` in `docker-compose.yml`. Local runs use the default
profile.

## OpenAPI documentation

### How it works

1. The Gradle convention plugin `devault.springdoc` adds `springdoc-openapi-starter-webmvc-ui`
   (version from the catalog) and `common-lib` to the service.
2. `common-lib`'s `OpenApiAutoConfiguration` activates only when springdoc is on the classpath, the app is
   a servlet web app and `springdoc.api-docs.enabled` is not `false`. It provides:
   - an `OpenAPI` bean (`@ConditionalOnMissingBean`) with title = `spring.application.name`,
     version = `spring.application.version` (fallback `v0`),
   - the `bearerAuth` security scheme (HTTP bearer, JWT) required globally for all operations,
   - hiding of every `@AuthenticationPrincipal` parameter (e.g. `AuthenticatedUser`) from the spec,
   - readable operation names (used by Insomnia as folders and request names): tag = controller name
     without `-controller` (`WorkspaceMemberController` → `workspace-member`), summary = method name split
     into words (`findWorkspaceById` → `find workspace by id`).
3. Each service opens the docs paths via `authlib.security.public-paths` and disables them in
   `application-prod.properties`.

### URLs (local)

| Service           | OpenAPI JSON                              | Swagger UI                                  |
|-------------------|-------------------------------------------|---------------------------------------------|
| auth-service      | http://localhost:8081/v3/api-docs         | http://localhost:8081/swagger-ui.html        |
| workspace-service | http://localhost:8080/api/v1/v3/api-docs  | http://localhost:8080/api/v1/swagger-ui.html |
| ingestion-service | http://localhost:8082/api/v1/v3/api-docs  | http://localhost:8082/api/v1/swagger-ui.html |

YAML is available under `/v3/api-docs.yaml`.

### Insomnia

`make insomnia` generates one ready-to-import **Devault** collection from the running services
(folders per service and controller, per-service `base_url`, pre-filled bodies, tokens saved after login,
path parameters filled from ids saved from responses).
See `insomnia/README.md`. The generated file is git-ignored.
A new service only needs one entry (name → spec URL) in `DEFAULT_SPECS` in `insomnia/generate.py`.

### Adding docs to a new service

1. `build.gradle.kts`:
   ```kotlin
   plugins {
       // ...
       id("devault.springdoc")
   }
   ```
2. `application.properties`:
   ```properties
   authlib.security.public-paths=/v3/api-docs/**,/swagger-ui/**,/swagger-ui.html
   ```
   If the service has its own `SecurityFilterChain`, permit `AuthLibSecurityProperties.publicPaths` there
   too (see `auth-service` `SecurityConfig`).
3. `application-prod.properties`:
   ```properties
   springdoc.api-docs.enabled=false
   springdoc.swagger-ui.enabled=false
   ```
4. Add `SPRING_PROFILES_ACTIVE: prod` to the service in `docker-compose.yml`.
5. Add the service's spec URL to `DEFAULT_SPECS` in `insomnia/generate.py`.

### Customizing

- Prefer an `OpenApiCustomizer` bean in the service – it is applied on top of the shared `OpenAPI` bean.
- Defining your own `OpenAPI` bean replaces the shared one entirely (including `bearerAuth`).
- Public endpoints can opt out of the global requirement with an empty `@SecurityRequirements` annotation.
- `@Tag(name = ...)` on a controller or `@Operation(summary = ...)` on a method overrides the generated names.
- Add `@Schema(example = ...)` to request DTO fields – Insomnia uses `example` values to pre-fill request
  bodies (otherwise it inserts placeholders like `"string"`).

# Devault – deployment modes

**Status: idea, not scheduled.** Nothing described under "Mode B" is implemented. This note records how a
hybrid deployment would work, what the current code already supports, and what would have to be added. It is
meant to be picked up after the roadmap in `AGENTS.md` is finished (gateway, frontend, K8s).

Written 2026-10-05, against Spring Boot 4.1.1 and the code on the `ingestion-service` branch.

## Summary

Devault can be run in two ways. The code is the same in both; they differ in configuration and in who
operates which part.

- **Mode A, fully self-hosted.** A team runs everything on its own machine, including its own auth-service.
  This works today.
- **Mode B, hybrid.** The project runs one central auth-service and the frontend. Each team runs the data
  services on its own server. The team's secrets, GitHub tokens and ingested data never leave that server.

Mode A carries no risk for the project author, who hosts nothing. Mode B makes the author the operator of a
login service with real accounts; that is where the responsibilities in "Risks of running the central part"
come from.

## Mode A – fully self-hosted

```
            team's own server
┌──────────────────────────────────────────┐
│ frontend                                 │
│ auth-service  (own users, own JWT keys)  │
│ workspace-service                        │
│ ingestion-service                        │
│ embedding-service, query-service         │
│ PostgreSQL, Redis, Kafka, vector store   │
└──────────────────────────────────────────┘
```

Start it with `make prod-build` (see `README.md`). The team generates its own Ed25519 key pair
(`make jwt-keys`) and its own `INGESTION_TOKEN_ENCRYPTION_KEY`.

## Mode B – hybrid

Two parts with different owners:

- **Control plane**, run by the project: the frontend, auth-service, and a registry of connected servers.
- **Data plane**, run by each team: every service that stores or processes the team's data.

```
     central (run by the project)                    team's own server
┌──────────────────────────────────┐        ┌────────────────────────────────────┐
│ frontend (static site)           │        │ workspace-service (see decision)   │
│ auth-service                     │◀───────│ ingestion-service                  │
│   /.well-known/jwks.json         │  JWKS  │ embedding-service, query-service   │
│ server registry                  │        │ PostgreSQL, Kafka, vector store    │
└────────────────▲─────────────────┘        └──────────────────▲─────────────────┘
                 │ login, token                                │ API calls with the token
                 │                                             │
              browser ─────────────────────────────────────────┘
```

Request flow:

1. The user opens the central frontend and logs in at the central auth-service.
2. The frontend asks the registry which server the user's workspace lives on, and gets a token that is valid
   for that server only.
3. The browser calls the team's server directly, with the token in the `Authorization` header. The central
   part is not in the path of these calls and never sees the request or the response.
4. The team's server validates the token offline: it downloads the public key from the central JWKS endpoint
   (cached), then checks the signature, the issuer, the expiry and the audience.
5. Data returned by the team's server goes straight to the browser.

The only call from the team's server to the central part is the JWKS download.

## Where things live

| Thing                                         | Mode A     | Mode B                         |
|-----------------------------------------------|------------|--------------------------------|
| User accounts, password hashes                | own server | central                        |
| JWT signing key (private)                     | own server | central                        |
| Refresh-token blacklist (Redis)               | own server | central                        |
| GitHub PATs (AES-GCM) and their encryption key | own server | own server                     |
| Ingested documents, embeddings                | own server | own server                     |
| Workspaces and member roles                   | own server | open decision, see below       |
| Frontend code                                 | own server | central                        |
| Address of the team's server                  | –          | central (registry)             |

## What the current code already supports

- **Token validation without a shared secret.** Resource services verify JWTs with the public key fetched from
  JWKS (`auth-service-lib`: `JwksClient`, `JwtClaimsService`). They need `authlib.jwks.uri` and
  `authlib.jwt.issuer`, never the private key.
- **One database per service, no JPA relations across services.** A service can be moved to another machine
  without touching the others' data.
- **Only auth-service uses Redis in code.** Resource services do not depend on the central blacklist.
- **Secrets stay with the operator.** PATs are encrypted with `INGESTION_TOKEN_ENCRYPTION_KEY`, which comes from
  the environment of the machine that runs ingestion-service.
- **Configuration through environment variables**, so the same image can point at a local or a central auth.
- **Versioned API prefix** (`/api/v1`), which a central frontend needs to talk to servers of different ages.

## What has to be added for Mode B

### 1. Audience (`aud`) in tokens – the most important change

Today `JwtClaimsService` checks only the issuer. A token issued by the central auth is therefore accepted by
every server that trusts that issuer. The operator of one server receives users' tokens and could replay them
against another server where the same user has access.

- auth-service puts the target server's id into `aud` when it issues a token.
- `auth-service-lib` gets a property with the server's own id and rejects tokens with a different `aud`.
- Consequence: the frontend needs one token per server. The mechanism (token exchange, or a per-server step at
  login) is an open decision.

### 2. Short-lived access tokens

Access tokens are not blacklisted on logout (DEV-6), and a remote server could not read the central Redis
anyway. The only protection is a short lifetime (`JWT_ACCESS_EXPIRATION`), with refresh handled centrally.

### 3. Key rotation with `kid`

`JwksClient` caches with a TTL and does not match on `kid`. With many remote servers depending on one signing
key, rotation has to work without downtime: publish the new key in JWKS first, match by `kid`, retire the old
key after the longest token lifetime has passed.

### 4. CORS on the team's services

The browser calls the team's server from the central frontend's origin. No service has a CORS configuration
today. It must be an allow-list containing the central origin, not a wildcard. Tokens travel in the
`Authorization` header, not in cookies, so no credentialed CORS is needed.

### 5. HTTPS on the team's server

A page served over `https` may not call an `http` address; `http://localhost` is the usual exception. A server
on a LAN address or a VPS needs a certificate. This is the main practical obstacle for non-technical users.

### 6. Server registry

The central part has to know which server a workspace lives on, so the frontend knows where to send requests.
Registering a server also creates the id used as `aud`.

### 7. Placement of workspace-service – open decision

- **On the team's server (recommended starting point).** Membership stays with the data. The role check from
  ING-9 is a call between two services on the same machine. The central part knows identities only. Cost:
  inviting someone needs a way to look up a user id by e-mail at the central auth.
- **Central.** One place for all workspaces, simpler invitations. Cost: every role check is a call from the
  team's server to the central part, which adds a dependency and a service-to-service authentication problem.

### 8. Configurable JWKS address

`authlib.jwks.uri` is currently `http://${JWKS_HOST:localhost}:8081/.well-known/jwks.json`: the scheme and the
port are fixed, only the host is configurable. Mode B needs the whole URL from configuration, over `https`.

### 9. Packaging and compatibility

No separate "release without auth" is needed. Every service is already its own image (one `Dockerfile` per
service), so a release is the same set of versioned images in both modes. The modes differ only in which
containers are started and which auth the configuration points at.

| Who runs it         | Containers                                                             |
|---------------------|------------------------------------------------------------------------|
| Mode A, the team    | all services including auth-service, PostgreSQL, Redis, Kafka          |
| Mode B, the team    | data services, PostgreSQL, Kafka                                       |
| Mode B, the project | auth-service, its PostgreSQL database, Redis, frontend                 |

How to select the containers:

- **Docker Compose: profiles.** Today the three services share the `prod` profile and the infrastructure has
  none. Mode B needs a second profile that leaves out auth-service. Redis has no profile and always starts; it
  needs one too, because only auth-service uses Redis.
- **Kubernetes: one Helm chart** with a switch in `values.yaml` (for example `auth.enabled`), which is the
  standard way to make a component optional.
- **A second example env file** for Mode B with the three settings that differ: the JWKS URL, the issuer, and
  the server's own id (the value expected in `aud`).

Rules:

- "Without auth" means without auth-service, not without `auth-service-lib`. The library stays in every
  service, because it is the part that validates tokens.
- A service never knows which mode it runs in. There is no mode flag in code, only configuration that says
  whom it trusts.
- **Token compatibility.** The central auth is always newer than the teams' servers, which update rarely.
  Claims may be added; existing claims may not be removed or change meaning.
- **API compatibility.** The central frontend will meet servers running older versions. Keep the API under
  `/api/v1`, and add an endpoint that reports the server's version.

Cleanup needed first: workspace-service declares the Redis starter and connection properties although no code
uses them. Remove them before building the Mode B bundle, otherwise the team's server still needs Redis
settings.

### 10. Protection of the central login

Rate limiting on login and registration, since the central auth is reachable by everyone.

## Limits of "everything stays on my server"

Mode B keeps data and secrets on the team's server, with three limits worth stating to users:

- **Frontend code.** The JavaScript comes from the central site and runs in the browser with the token. It can
  technically read whatever it displays. Users still have to trust the site.
- **Identity.** The central auth knows the accounts, when users log in, and the address of their server.
- **LLM calls.** If query-service sends document chunks to an external model, that data leaves the server.
  Full privacy needs a local model or the team's own API key.

## Risks of running the central part

- **Breach.** Whoever obtains the central private key can mint tokens for every connected server.
- **Availability.** When the central auth is down, nobody can log in to their own server.
- **Personal data.** Storing accounts of EU users falls under GDPR. This is not legal advice; check it before
  accepting real users.
- **Self-written auth.** The current auth-service is good for learning. As a login service for other people it
  is the largest risk in this design. Before real users: replace it with a proven identity provider (for
  example Keycloak) or have it reviewed.

## Rules to keep now

These cost nothing today and keep Mode B possible:

- Resource services never read the auth database or the auth Redis.
- Every address of another service, and the trusted issuer, comes from configuration.
- No code branches on the deployment mode.
- Resource services store user ids only, not profile data.
- The API stays under `/api/v1`.

## Suggested order

1. Finish the roadmap in Mode A.
2. Build Mode B as an exercise, with own test accounts only.
3. Accept real users only after replacing or reviewing the auth and checking the legal side.

Steps 1 and 2 carry no risk and contain all of the learning. The risk starts at step 3.

## Open decisions

- Where workspace-service lives (section 7).
- How the frontend obtains a token per server (section 1).
- How a team registers its server, and how its address is verified.
- Which identity provider backs the central login for real users.

## Not verified

- Current behaviour of Chrome's Private Network Access rules for a public site calling a private address.
- How Safari treats `http://localhost` when called from an `https` page.

## Reading

- Spring Security reference, "OAuth 2.0 Resource Server" (JWT, JWKS, issuer and audience validation).
- RFC 7519, JSON Web Token: the `aud` claim.
- RFC 8707, Resource Indicators for OAuth 2.0: requesting a token for one specific server.
- RFC 8693, OAuth 2.0 Token Exchange.
- Docker Compose documentation, "Using profiles with Compose".
- Helm documentation, "Chart dependencies" (the `condition` field) for optional components.

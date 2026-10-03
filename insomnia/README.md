# Insomnia collection

`generate.py` builds an Insomnia import file from the running services' OpenAPI specs.
The generated file (`devault.insomnia.json`) is not committed – generate it locally.

## Generate

Requirements: Python 3 (no extra packages), services running locally.

```bash
make insomnia
# or
python3 insomnia/generate.py
```

Services that are not running are skipped with a warning. Other ports / hosts:

```bash
python3 insomnia/generate.py --spec workspace=http://localhost:18080/api/v1/v3/api-docs
```

## Import

Insomnia → your project → **Import** → **File** → `insomnia/devault.insomnia.json`.

You get one **Devault** collection:

```
Devault
├─ auth        (base_url = http://localhost:8081)
│  ├─ auth
│  └─ jwks
├─ workspace   (base_url = http://localhost:8080/api/v1)
│  ├─ workspace
│  └─ workspace-member
└─ ingestion   (base_url = http://localhost:8082/api/v1)
   ├─ credential
   └─ ingestion-source
```

- `base_url` is set per service folder (folder environment).
- Request bodies are pre-filled from `@Schema(example = ...)` in the DTOs.
- **login** and **refresh** save the returned tokens to `bearerToken` and `refreshToken`
  (after-response script); other requests use `Bearer {{ bearerToken }}`, refresh/logout send
  `{{ refreshToken }}`. Just call **login** first.
- Path parameters (`:id`, `:workspaceId`) are filled from id variables in the base environment
  (`workspaceId`, `memberId`, ...). Requests that return a resource or a list save its id there
  (after-response script): a created resource always, a list only when the saved id is not on it
  (then the first item). Just call **save workspace** or **find all workspaces** first.
  To use another id, change the variable in the base environment or the value in the request's **Params** tab.
- **save credential** sends `{{ githubToken }}` as the token. To have it pre-filled on every generation, set
  your GitHub PAT in `.env.local` (or `.env`; an environment variable of the same name wins):

  ```bash
  INSOMNIA_GITHUB_TOKEN=ghp_...
  ```

  The generated file then contains the token – it is git-ignored, do not commit or share it.
  Without the setting `githubToken` stays empty and can be filled in the base environment.

## New service

Add one line to `DEFAULT_SPECS` in `generate.py`, e.g.:

```python
"query": "http://localhost:8083/api/v1/v3/api-docs",
```

Everything else (folders, request names, bodies, token handling) is derived from the spec.

## After API changes

Regenerate and import again. Request ids are deterministic, so Insomnia updates the existing requests
instead of creating duplicates.

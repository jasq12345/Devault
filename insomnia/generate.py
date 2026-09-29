#!/usr/bin/env python3
"""
Generates an Insomnia import file (export format v4) with one "Devault" collection
built from the running services' OpenAPI specs.

Layout: Devault / <service> / <controller tag> / <request>
- each service folder has its own `base_url` (folder environment), so all services live in one collection
- request bodies are pre-filled from `example` values in the specs
- login/refresh store the returned tokens in `bearerToken` / `refreshToken` (after-response script)

Usage: see insomnia/README.md
"""
import argparse
import hashlib
import json
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

DEFAULT_SPECS = {
    "auth": "http://localhost:8081/v3/api-docs",
    "workspace": "http://localhost:8080/api/v1/v3/api-docs",
    "ingestion": "http://localhost:8082/api/v1/v3/api-docs",
}
DEFAULT_OUTPUT = Path(__file__).parent / "devault.insomnia.json"

WORKSPACE_ID = "wrk_devault"
BASE_ENV_ID = "env_devault_base"
BEARER_SCHEME = "bearerAuth"
METHOD_ORDER = ["get", "post", "put", "patch", "delete"]

STORE_TOKENS_SCRIPT = """\
// Generated: stores tokens from the response for the other requests
const body = insomnia.response.json();
if (body && body.data && body.data.accessToken) {
  insomnia.baseEnvironment.set("bearerToken", body.data.accessToken);
  insomnia.baseEnvironment.set("refreshToken", body.data.refreshToken);
}
"""


def stable_id(prefix: str, *parts: str) -> str:
    # Deterministic ids, so re-importing a regenerated file updates the same requests
    return f"{prefix}_{hashlib.sha1('/'.join(parts).encode()).hexdigest()[:24]}"


def fetch_spec(url: str) -> dict:
    with urllib.request.urlopen(url, timeout=10) as response:
        return json.load(response)


def resolve(doc: dict, node):
    """Recursively resolves local $ref pointers."""
    if isinstance(node, dict):
        if "$ref" in node:
            target = doc
            for part in node["$ref"].lstrip("#/").split("/"):
                target = target[part]
            return resolve(doc, target)
        return {key: resolve(doc, value) for key, value in node.items()}
    if isinstance(node, list):
        return [resolve(doc, value) for value in node]
    return node


def non_null(schemas: list) -> list:
    return [s for s in schemas if s.get("type") != "null"]


def example_value(schema: dict):
    if not isinstance(schema, dict):
        return None
    if "example" in schema:
        return schema["example"]
    if schema.get("examples"):
        return schema["examples"][0]
    if "default" in schema:
        return schema["default"]
    if schema.get("enum"):
        return schema["enum"][0]
    for key in ("oneOf", "anyOf"):
        if schema.get(key):
            candidates = non_null(schema[key]) or schema[key]
            return example_value(candidates[0])
    if schema.get("allOf"):
        merged = {}
        for sub in schema["allOf"]:
            value = example_value(sub)
            if isinstance(value, dict):
                merged.update(value)
        return merged

    schema_type = schema.get("type")
    if isinstance(schema_type, list):  # OpenAPI 3.1: ["string", "null"]
        schema_type = next((t for t in schema_type if t != "null"), None)
    schema_format = schema.get("format")

    if schema_type == "object" or "properties" in schema:
        return {name: example_value(prop) for name, prop in (schema.get("properties") or {}).items()}
    if schema_type == "array":
        return [example_value(schema.get("items", {}))]
    if schema_type == "string":
        return {
            "uuid": "00000000-0000-0000-0000-000000000000",
            "email": "user@example.com",
            "date-time": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        }.get(schema_format, "")
    if schema_type in ("integer", "number"):
        return 0
    if schema_type == "boolean":
        return False
    return None


def json_body(doc: dict, operation: dict):
    content = (operation.get("requestBody") or {}).get("content", {})
    media_type = next((m for m in content if "json" in m), None)
    if media_type is None:
        return None
    body = example_value(resolve(doc, content[media_type].get("schema", {})))
    if isinstance(body, dict) and "refreshToken" in body:
        body["refreshToken"] = "{{ _.refreshToken }}"
    return body


def returns_tokens(doc: dict, operation: dict) -> bool:
    """True when the success response looks like ApiResponse<TokenPair>."""
    for code, response in (operation.get("responses") or {}).items():
        if not code.startswith("2"):
            continue
        for media in (response.get("content") or {}).values():
            data = resolve(doc, media.get("schema", {})).get("properties", {}).get("data", {})
            candidates = non_null(data.get("oneOf") or data.get("anyOf") or [data])
            if any("accessToken" in (c.get("properties") or {}) for c in candidates):
                return True
    return False


def requires_bearer(doc: dict, operation: dict) -> bool:
    security = operation.get("security", doc.get("security", []))
    return any(BEARER_SCHEME in requirement for requirement in security)


def build_request(doc: dict, service: str, folder_id: str, path: str, method: str, operation: dict, sort_key: int) -> dict:
    parameters = [resolve(doc, p) for p in operation.get("parameters", [])]
    body = json_body(doc, operation)
    request = {
        "_id": stable_id("req", service, method, path),
        "_type": "request",
        "parentId": folder_id,
        "name": operation.get("summary") or operation.get("operationId") or path,
        "description": operation.get("description", ""),
        "method": method.upper(),
        "url": "{{ _.base_url }}" + re.sub(r"{([^}]+)}", r":\1", path),
        "pathParameters": [
            {"name": p["name"], "value": str(p.get("example", ""))} for p in parameters if p.get("in") == "path"
        ],
        "parameters": [
            {"name": p["name"], "value": str(p.get("example", "")), "disabled": not p.get("required", False)}
            for p in parameters if p.get("in") == "query"
        ],
        "headers": [{"name": "Content-Type", "value": "application/json"}] if body is not None else [],
        "body": {"mimeType": "application/json", "text": json.dumps(body, indent=2, ensure_ascii=False)}
        if body is not None else {},
        "authentication": {"type": "bearer", "token": "{{ _.bearerToken }}", "prefix": ""}
        if requires_bearer(doc, operation) else {},
        "metaSortKey": sort_key,
    }
    if returns_tokens(doc, operation):
        request["afterResponseScript"] = STORE_TOKENS_SCRIPT
    return request


def build_service(doc: dict, service: str, sort_key: int) -> list:
    service_folder_id = stable_id("fld", service)
    resources = [{
        "_id": service_folder_id,
        "_type": "request_group",
        "parentId": WORKSPACE_ID,
        "name": service,
        "description": "",
        "environment": {"base_url": doc["servers"][0]["url"]},
        "environmentPropertyOrder": None,
        "metaSortKey": sort_key,
    }]

    operations = sorted(
        (
            (path, method, operation)
            for path, item in doc.get("paths", {}).items()
            for method, operation in item.items()
            if method in METHOD_ORDER
        ),
        key=lambda op: (op[0], METHOD_ORDER.index(op[1])),
    )
    tag_of = lambda operation: (operation.get("tags") or [service])[0]
    tag_names = sorted({tag_of(operation) for _, _, operation in operations})
    tag_folders = {}
    for index, (path, method, operation) in enumerate(operations):
        tag = tag_of(operation)
        if tag not in tag_folders:
            tag_folders[tag] = stable_id("fld", service, tag)
            resources.append({
                "_id": tag_folders[tag],
                "_type": "request_group",
                "parentId": service_folder_id,
                "name": tag,
                "description": "",
                "environment": {},
                "environmentPropertyOrder": None,
                "metaSortKey": tag_names.index(tag),
            })
        resources.append(build_request(doc, service, tag_folders[tag], path, method, operation, index))
    return resources


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate an Insomnia collection from the services' OpenAPI specs.")
    parser.add_argument(
        "--spec", action="append", default=[], metavar="NAME=URL",
        help="override or add a service spec URL, e.g. --spec workspace=http://localhost:18080/api/v1/v3/api-docs",
    )
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help=f"output file (default: {DEFAULT_OUTPUT})")
    args = parser.parse_args()

    specs = dict(DEFAULT_SPECS)
    for override in args.spec:
        name, _, url = override.partition("=")
        specs[name] = url

    resources = [
        {"_id": WORKSPACE_ID, "_type": "workspace", "parentId": None, "name": "Devault", "description": "",
         "scope": "collection"},
        {"_id": BASE_ENV_ID, "_type": "environment", "parentId": WORKSPACE_ID, "name": "Base Environment",
         "data": {"bearerToken": "", "refreshToken": ""}, "dataPropertyOrder": None, "isPrivate": False},
    ]
    included = []
    for sort_key, (service, url) in enumerate(specs.items()):
        try:
            doc = fetch_spec(url)
        except (urllib.error.URLError, OSError) as error:
            print(f"skipped {service}: cannot fetch {url} ({error})", file=sys.stderr)
            continue
        resources.extend(build_service(doc, service, sort_key))
        included.append(service)

    if not included:
        print("no service available - start the services first", file=sys.stderr)
        return 1

    export = {
        "_type": "export",
        "__export_format": 4,
        "__export_date": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "__export_source": "devault.insomnia-generator",
        "resources": resources,
    }
    args.output.write_text(json.dumps(export, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    requests = sum(1 for r in resources if r["_type"] == "request")
    print(f"wrote {args.output} ({', '.join(included)}; {requests} requests)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

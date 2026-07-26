# Deployment — CI/CD

## Alcance

CI/CD de **Atlas como producto** (este repo), distinto de los Pipelines que Atlas ejecuta para los projects del cliente.

## Pipeline propuesto (GitHub Actions / Gitea Actions)

```text
on: pull_request, push main
  ├─ backend: ./gradlew test
  ├─ frontend: npm ci && npm run build (y lint)
  ├─ docker build backend + frontend (tags sha)
  └─ (main) push registry + deploy hook opcional
```

## Entornos

| Env | Trigger | Notas |
|-----|---------|-------|
| local | compose | SSO off |
| staging | push main / tag | SSO on, datos non-prod |
| production | tag `v*` + approval | manual promote |

## Calidad de gates

- Tests unit + integration (Testcontainers).
- No secretos en imágenes (multi-stage ya en Dockerfiles).
- Scan opcional Trivy en CI (v0.6+).
- Versionar OpenAPI artifact.

## Deploy de Atlas en el servidor

Opciones (elegir una, documentar en runbook interno):

1. `docker compose pull && docker compose up -d` vía SSH desde CI (simple).
2. Watchtower / script cron en host (menos control).
3. Atlas **self-managing** (meta): registrar Atlas como Project — útil tarde; riesgo de pie-en-bala; no v0.x.

## Release notes

Cada versión de producto (v0.1…v1.0) debe listar: migraciones Flyway, deprecations API, feature flags nuevas.

## Pipelines de cliente (producto)

Documentados en módulos Delivery: webhooks Git → jobs worker. No confundir con CI de este repositorio.

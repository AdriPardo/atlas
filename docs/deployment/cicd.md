# Deployment — CI/CD

## Alcance

CI/CD de **Atlas como producto** (este repo), distinto de los Pipelines que Atlas ejecuta para los projects del cliente.

## Deploy automático a producción (activo)

Ver **[cicd-atlas-self.md](./cicd-atlas-self.md)**: GitHub Actions → SSH → `scripts/deploy.sh` en la VM (`push` a `master`).

## Pipeline de calidad propuesto

```text
on: pull_request, push master
  ├─ backend: ./gradlew test
  ├─ frontend: npm ci && npm run build (y lint)
  ├─ docker build backend + frontend (tags sha)
  └─ (master) deploy producción vía workflow deploy-production.yml
```

## Entornos

| Env | Trigger | Notas |
|-----|---------|-------|
| local | compose | SSO off |
| staging | push / tag | SSO on, datos non-prod |
| production | push `master` + self-hosted runner | ver cicd-atlas-self.md |

## Calidad de gates

- Tests unit + integration (Testcontainers).
- No secretos en imágenes (multi-stage ya en Dockerfiles).
- Scan opcional Trivy en CI (v0.6+).
- OpenAPI artifact versionado: [`docs/api/openapi.json`](../api/openapi.json) (regenerar vía `OpenApiContractIntegrationTest` + `-Datlas.writeOpenApi=true`).

## Release notes

Cada versión de producto (v0.1…v1.0) debe listar: migraciones Flyway, deprecations API, feature flags nuevas.

## Pipelines de cliente (producto)

Documentados en módulos Delivery: webhooks Git → jobs worker. No confundir con CI de este repositorio.

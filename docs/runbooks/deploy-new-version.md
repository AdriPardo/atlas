# Runbook — Desplegar una nueva versión

**Estado:** plantilla (pipeline CI/CD **NO ENCONTRADO** en Git)

## Hoy

No hay workflow GitHub Actions ni scripts de deploy en el repo auditado.

## Procedimiento mínimo propuesto

1. Cambios en rama feature → PR → review.
2. Merge a `master` (o rama de release definida).
3. En host: `git pull` del árbol de configs (ruta TBD) o sync controlado.
4. `docker compose up -d` del stack afectado.
5. Verificación post-deploy (health, smoke test, dashboards).

## Mejoras futuras

- CI de validación (`compose config`, lint YAML)
- CD con approvals
- Ventanas de cambio y checklist

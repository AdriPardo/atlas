# Grafana

**Estado de ficha:** BLOQUEADO — sin evidencia en Git ni acceso al host  
**Fuente del nombre:** briefing de inventario (NO VERIFICADO en runtime)

## Descripción

Visualización y dashboards (citado en briefing)

No hay archivos de despliegue de este servicio en el repositorio auditado (`bf51909`).

## Arquitectura

**NO VERIFICADO.** Ver [../architecture/logical.md](../architecture/logical.md).

## Docker Compose

| Campo | Valor |
| --- | --- |
| Archivo compose | NO ENCONTRADO |
| Servicio compose | NO ENCONTRADO |
| Imagen | NO ENCONTRADO |
| Tag / digest | NO ENCONTRADO |
| Restart policy | NO ENCONTRADO |
| Depends on | NO ENCONTRADO |

## Configuración

**NO ENCONTRADO** en Git. Pendiente de recolección en host.

## Persistencia

| Volumen / bind | Ruta | Propósito | Evidencia |
| --- | --- | --- | --- |
| — | — | — | BLOQUEADO |

## Backup

**NO ENCONTRADO** procedimiento ni job en Git.

## Restore

**NO ENCONTRADO.** Runbook genérico pendiente de validación: [../runbooks/restore-backup.md](../runbooks/restore-backup.md).

## Alertas

**NO ENCONTRADO** reglas asociadas en Git.

## Dashboards

**NO ENCONTRADO** exports JSON / provisioning en Git.

## Troubleshooting

1. Confirmar que el contenedor existe: `docker ps -a | grep -i grafana` (**requiere host**)
2. Inspeccionar: `docker inspect <container>`
3. Revisar logs: `docker logs --tail 200 <container>`
4. Completar esta ficha con evidencia de `inventory/raw/host/`

## Dependencias

**NO VERIFICADO.**

## Mejoras futuras

- Importar compose/config al repo (sin secretos)
- Fijar tags de imagen inmutables (digest)
- Documentar healthcheck, backup y restore con pruebas reales
- Enlazar alertas y dashboards concretos cuando existan

## Criticidad (estimación pendiente)

Sin runtime no se asigna criticidad operativa. Tras inventario: clasificar como `critical` / `high` / `medium` / `low`.

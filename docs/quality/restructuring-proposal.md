# Propuesta de reestructuración

**Estado:** PROPUESTA — **no aplicada** al host ni a paths productivos  
**Regla:** esperar aprobación explícita antes de mover runtime

## Problema

Hoy el repo no refleja la infraestructura. Cuando el host sea accesible, conviene un layout estable para años de crecimiento.

## Layout propuesto (Git)

```text
atlas/
  README.md
  docs/                          # ya creado
  inventory/raw/                 # evidencia de auditoría (gitignorada si contiene datos sensibles)
  scripts/
    inventory/
  stacks/
    edge/
      traefik/
      cloudflare-tunnel/
    observability/
      prometheus/
      alertmanager/
      grafana/
      loki/
      alloy/
      exporters/                 # node-exporter, cadvisor
    platform/                    # futuros servicios de plataforma
  .env.example                   # solo nombres / placeholders
  .github/workflows/
    validate-compose.yml
```

## Principios

1. Un directorio = un stack compose cohesivo
2. Secretos fuera de Git (o secret manager); en Git solo nombres
3. Imágenes pinneadas
4. Redes explícitas (`edge`, `observability`, `internal`)
5. Docs de servicio obligatorias en el mismo PR que el compose
6. No mover volúmenes existentes sin plan de migrate + backup

## Migración sugerida (cuando haya aprobación + acceso)

1. Inventariar paths actuales en el host (no mover nada).
2. Mapear path actual → path propuesto.
3. Copiar configs a Git (redactadas).
4. Validar `docker compose config`.
5. Plan de cutover por stack (edge primero o último según dependencia).
6. Solo entonces cambiar paths en el host.

## Qué ya se hizo sin riesgo

- Crear `docs/`, ADRs, runbooks plantilla, script de recolección de solo lectura.

## Qué NO se hará sin aprobación

- Renombrar directorios en el servidor
- Recrear redes/volúmenes
- Cambiar Traefik/Tunnel en caliente
- Borrar “servicios obsoletos” no confirmados

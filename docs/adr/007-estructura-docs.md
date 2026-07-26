# ADR-007 — Estructura de documentación `docs/`

## Estado

Aceptada (2026-07-26)

## Contexto

Un único README enorme no escala. Atlas necesita documentación auditable por dominio.

## Decisión

Organizar en:

```
docs/
  architecture/
  monitoring/
  logging/
  networking/
  security/
  services/
  runbooks/
  adr/
  operations/
  quality/
```

Índice en `docs/README.md`. Evidencia cruda en `inventory/raw/`.

## Consecuencias

- Navegación predecible
- Fichas de servicio uniformes
- Separación entre hechos (`operations/inventory`) y propuestas (`quality/restructuring-proposal`)

# ADR-000 — Documentar solo con evidencia

## Estado

Aceptada (2026-07-26)

## Contexto

Atlas debe prepararse para auditoría y crecimiento plurianual. Inventar compose, versiones o flujos invalidaría la documentación.

## Decisión

Toda afirmación de runtime debe basarse en archivos reales o comandos de recolección. Lo ausente se marca **NO ENCONTRADO** / **NO VERIFICADO** / **BLOQUEADO**.

## Consecuencias

- Documentación incompleta es preferible a documentación falsa
- Las fichas de servicio pueden existir en estado bloqueado
- La Fase 2 (inventario host) actualizará ADRs de stack a “Aceptada” solo con evidencia

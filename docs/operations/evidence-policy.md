# Política de evidencia

## Regla

Toda afirmación operativa debe citar:

1. ruta de archivo en Git, o
2. ruta en el host + comando de recolección, o
3. etiqueta **NO ENCONTRADO** / **NO VERIFICADO** / **BLOQUEADO**

## Prohibido

- Inferir versiones de imagen sin leer compose o `docker inspect`
- Dibujar flujos de red “típicos” como si fueran los de Atlas
- Documentar backups/alertas/dashboards sin archivos o export reales
- Completar fichas de servicio con valores de tutoriales externos

## Permitido

- Declarar objetivos de plataforma tal como aparecen en el briefing, etiquetados como **declarado / no verificado**
- Proponer estructuras y ADRs de documentación (decisión del proceso de auditoría)
- Dejar secciones vacías con `Pendiente de verificación en host`

# Documentación Atlas

Documentación técnica organizada por dominio. Cada documento es corto, enlazado y basado en evidencia.

## Mapa

| Área | Ruta | Contenido |
| --- | --- | --- |
| Arquitectura | [architecture/](architecture/README.md) | Física, lógica, flujos |
| Monitorización | [monitoring/](monitoring/README.md) | Métricas, scrape, alertas |
| Logging | [logging/](logging/README.md) | Pipeline de logs |
| Networking | [networking/](networking/README.md) | Redes Docker, proxy, túnel |
| Seguridad | [security/](security/README.md) | Secretos, exposición, certificados |
| Servicios | [services/](services/README.md) | Una ficha por servicio |
| Runbooks | [runbooks/](runbooks/README.md) | Procedimientos operativos |
| ADR | [adr/](adr/README.md) | Decisiones de arquitectura |
| Operaciones | [operations/](operations/README.md) | Inventario, acceso, auditoría |
| Calidad | [quality/](quality/README.md) | Hallazgos y propuestas |

## Lectura recomendada (auditoría)

1. [operations/audit-status.md](operations/audit-status.md) — estado de la misión de inventario
2. [operations/inventory.md](operations/inventory.md) — inventario técnico
3. [quality/findings.md](quality/findings.md) — riesgos y gaps
4. [quality/restructuring-proposal.md](quality/restructuring-proposal.md) — cambios propuestos (sin aplicar)

## Convención de estado

En toda la documentación se usan estas etiquetas:

- **VERIFICADO**: leído de un archivo real o comando ejecutado con éxito
- **NO ENCONTRADO**: se buscó y no existe en el alcance actual
- **NO VERIFICADO**: existe en el briefing o se espera, pero no hay evidencia accesible
- **BLOQUEADO**: no se pudo comprobar por falta de acceso o herramienta

# Seguridad

**Estado:** parcial (solo hallazgos del entorno de auditoría)

## Hallazgos verificados

| ID | Hallazgo | Severidad |
| --- | --- | --- |
| S-001 | Repositorio público sin código de infra, pero también sin secretos | Info |
| S-002 | Acceso SSH al host no provisionado en el agente de auditoría | Alto (bloquea control) |
| S-003 | No hay `.env.example` ni inventario de secretos | Medio |
| S-004 | No hay política documentada de certificados | Medio |

Detalle: [../quality/findings.md](../quality/findings.md)

## Pendiente en host

- Permisos de archivos de secretos
- Exposición de dashboards (Prometheus/Grafana) sin auth
- Contenido de tokens Cloudflare
- Cap-drop / read-only / user no-root en contenedores
- TLS termination point

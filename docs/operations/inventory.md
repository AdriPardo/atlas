# Inventario técnico Atlas

**Estado global:** BLOQUEADO para el host · VERIFICADO para el repositorio Git  
**Última actualización:** 2026-07-26 (UTC)

## 1. Inventario del repositorio Git (VERIFICADO)

| Path | Tipo | Notas |
| --- | --- | --- |
| `README.md` | archivo | Contenido exacto previo a esta rama: `# atlas` |
| `docs/` | directorio | Introducido en la rama de inventario/documentación |
| `scripts/inventory/` | directorio | Script de recolección remota |
| `inventory/raw/` | directorio | Evidencia cruda de auditoría del agente |

**No existen** en Git (búsqueda en árbol del commit base y working tree previo a docs):

- `docker-compose.yml` / `compose.yaml`
- directorios de stacks/servicios
- `prometheus.yml`, reglas Alertmanager, dashboards Grafana
- configs Loki / Alloy / Traefik / cloudflared
- `.env` / `.env.example`
- scripts de backup
- units systemd
- certificados

## 2. Inventario del entorno del agente (VERIFICADO)

| Componente | Valor observado |
| --- | --- |
| Hostname agente | `cursor` |
| OS | Linux 6.12.94+ x86_64 |
| Usuario | `ubuntu` |
| Docker CLI | no instalado |
| SSH keys (`~/.ssh`) | ausente |
| Acceso al host Atlas | no configurado |
| Entorno Cursor cloud | `null` |

Detalle: [`../../inventory/raw/agent-environment-probe.txt`](../../inventory/raw/agent-environment-probe.txt)

## 3. Inventario de servicios (host) — BLOQUEADO

Para cada servicio esperado se requiere evidencia del host. Mientras no exista, el campo queda **NO VERIFICADO**.

Plantilla usada (campos vacíos = sin evidencia):

| Campo | Valor |
| --- | --- |
| nombre | |
| propósito | |
| criticidad | |
| imagen docker | |
| versión | |
| puertos | |
| redes | |
| volúmenes | |
| variables importantes | |
| dependencias | |
| healthcheck | |
| backup | |
| restauración | |
| alertas existentes | |
| dashboard asociado | |
| documentación existente | |
| riesgos detectados | Ver [../quality/findings.md](../quality/findings.md) a nivel plataforma |
| mejoras recomendadas | Completar recolección remota |

### 3.1 Catálogo esperado (briefing) vs evidencia

| Servicio | En briefing | En Git | En host |
| --- | --- | --- | --- |
| Traefik | sí | NO ENCONTRADO | BLOQUEADO |
| Cloudflare Tunnel (cloudflared) | sí | NO ENCONTRADO | BLOQUEADO |
| Prometheus | sí | NO ENCONTRADO | BLOQUEADO |
| Alertmanager | sí | NO ENCONTRADO | BLOQUEADO |
| Grafana | sí | NO ENCONTRADO | BLOQUEADO |
| Loki | sí | NO ENCONTRADO | BLOQUEADO |
| Alloy | sí | NO ENCONTRADO | BLOQUEADO |
| Node Exporter | sí | NO ENCONTRADO | BLOQUEADO |
| cAdvisor | sí | NO ENCONTRADO | BLOQUEADO |
| Otros servicios no listados | — | NO ENCONTRADO | BLOQUEADO |

Fichas individuales: [../services/](../services/README.md)

## 4. Redes Docker — BLOQUEADO

| Red | Driver | Servicios | Evidencia |
| --- | --- | --- | --- |
| — | — | — | Sin acceso a `docker network ls` |

## 5. Volúmenes — BLOQUEADO

| Volumen | Servicio | Persistencia | Backup | Evidencia |
| --- | --- | --- | --- | --- |
| — | — | — | — | Sin acceso a `docker volume ls` |

## 6. Cron / systemd / certificados / backups — BLOQUEADO

| Ámbito | Resultado |
| --- | --- |
| Cron | BLOQUEADO (sin host) |
| systemd | BLOQUEADO (sin host) |
| Certificados | BLOQUEADO (sin host); en Git: NO ENCONTRADO |
| Backups | BLOQUEADO (sin host); en Git: NO ENCONTRADO |

## 7. Dependencias entre servicios — BLOQUEADO

No hay grafo verificable. Diagrama placeholder: [../architecture/logical.md](../architecture/logical.md)

## 8. Próximo paso para cerrar el inventario

1. Cumplir [access-requirements.md](access-requirements.md)
2. Ejecutar [host-collection.md](host-collection.md)
3. Rellenar fichas en `docs/services/` y tablas de este documento con citas de evidencia

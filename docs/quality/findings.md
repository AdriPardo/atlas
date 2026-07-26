# Hallazgos de calidad — Auditoría Atlas

**Fecha:** 2026-07-26  
**Alcance:** repositorio Git + entorno del Cloud Agent  
**Fuera de alcance (bloqueado):** runtime del servidor

## Resumen

| Severidad | Cantidad |
| --- | --- |
| Crítica | 2 |
| Alta | 3 |
| Media | 4 |
| Baja / Info | 2 |

## Hallazgos

### F-001 — Repositorio sin infraestructura versionada (Crítica)

**Evidencia:** commit `bf51909` solo contiene `README.md` (`# atlas`).  
**Impacto:** no hay IaC/compose auditable; el estado del sistema no es reproducible desde Git.  
**Recomendación:** importar stacks (sin secretos) al repo o a un monorepo de infra.

### F-002 — Sin acceso operativo al host desde el agente de auditoría (Crítica)

**Evidencia:** sin `~/.ssh`, sin `ATLAS_SSH_*`, `environment: null`, `usePrivateWorker: false`.  
**Impacto:** imposible inventariar contenedores, redes, volúmenes, cron, systemd.  
**Recomendación:** ver [../operations/access-requirements.md](../operations/access-requirements.md).

### F-003 — Documentación operativa inexistente previa a esta rama (Alta)

**Evidencia:** no había `docs/` en `master`.  
**Impacto:** conocimiento no transferible; riesgo de bus-factor = 1.  
**Mitigación iniciada:** estructura `docs/` en esta PR.

### F-004 — Backups no evidenciados (Alta)

**Evidencia:** ningún script/cron/doc de backup en Git; host no accesible.  
**Impacto:** posible pérdida total de Grafana/Prometheus/Loki sin plan de restore.  
**Recomendación:** inventariar backups reales; si no existen, tratarlo como incidente de continuidad.

### F-005 — Observabilidad citada pero no versionada (Alta)

**Evidencia:** briefing menciona Prometheus/Grafana/Loki/Alloy; cero configs en Git.  
**Impacto:** dashboards/reglas pueden vivir solo en volúmenes locales (efímero / no auditable).  
**Recomendación:** provisioning as code.

### F-006 — Secretos y variables no inventariados (Media)

**Evidencia:** no hay `.env.example` ni catálogo de secretos.  
**Recomendación:** listar nombres de variables (no valores) por servicio.

### F-007 — Sin CI de validación (Media)

**Evidencia:** no hay `.github/workflows`.  
**Recomendación:** al importar compose, añadir `docker compose config` / yamllint.

### F-008 — Nombres y layout de stacks desconocidos (Media)

**Evidencia:** sin árbol en host/repo.  
**Riesgo:** inconsistencias habituales (`docker-compose.yml` vs `compose.yml`, stacks mezclados).  
**Recomendación:** adoptar layout propuesto en [restructuring-proposal.md](restructuring-proposal.md) tras inventario.

### F-009 — Certificados / TLS no documentados (Media)

**Evidencia:** no hay material ni docs.  
**Recomendación:** documentar dónde termina TLS (Cloudflare vs Traefik).

### F-010 — Repo público vacío (Info)

**Evidencia:** `visibility: public`, size 0.  
**Nota:** no expone secretos hoy; al importar configs, revisar redaction.

### F-011 — Docker CLI ausente en el agente (Info)

**Evidencia:** `docker: command not found` en el agente.  
**Impacto:** incluso con socket remoto haría falta herramienta; el path previsto es SSH al host.

## Lo que no se oculta

No se ha verificado la existencia real de Traefik, Cloudflare Tunnel, Prometheus, Alertmanager, Grafana, Loki, Alloy, Node Exporter ni cAdvisor. Solo constan como **citados en el briefing**.

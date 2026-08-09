# ADR-0015 — Acceso a base de datos por Project (aislamiento)

- **Estado:** Accepted (contrato + envFrom + provisioner + TTL credentials opción C)
- **Fecha:** 2026-07-30

## Contexto

Atlas es plano de ops ([ADR-0002](ADR-0002-single-tenant-install.md): una org por install). Dentro de esa install conviven muchos projects/apps de cliente (p. ej. Reelpath). Hoy:

- Secrets de project/org + bindings (`git.token`, `cloudflare.api.token`) ya existen.
- `BACKUP_DATABASE` solo hace `pg_dump` de la **DB de Atlas** (control plane), no de DBs de apps.
- Módulo Databases en [runtime.md](../modules/runtime.md) está `designed`, sin registro tipado.
- Deploy inyecta env vía Compose / `.env`; migraciones son del repo (`runtime.migrateCommand`, [app-migrations.md](../deployment/app-migrations.md)) — Atlas **no** posee Prisma ni otro ORM ([ADR-0014](ADR-0014-project-manifest-runtime.md)).

Falta un contrato de producto: cómo un project obtiene acceso DB **solo** a sus datos, con permisos controlados, sin hardcodear ORM.

### Opciones evaluadas

| | Enfoque | Pros | Contras |
|---|---------|------|---------|
| **A** | Postgres roles + schemas (o DB dedicada) por project | Aislamiento en el motor; least privilege nativo; backups por schema/DB; bajo acoplamiento a Atlas en hot path | Provisioning roles/grants; convención de nombres; ops de cluster |
| **B** | Proxy SQL console Atlas + RLS | UI unificada; políticas en Atlas | Atlas en path de cada query; superficie ataque grande; ops/complejidad alta |
| **C** | Connection URLs / credenciales time-limited emitidas por Atlas | UX “click → connect”; rotación fácil | Requiere A (o equivalente) debajo; cron de revoke; más API |

## Decisión

1. **Modelo de aislamiento (norte):** **A** — por project, al menos un **schema** (default) o **database** dedicada (opt-in alto riesgo), con **rol Postgres** propio. Search_path / grants limitan al propio namespace. Atlas control plane DB (`atlas`) permanece separada y **nunca** se expone a projects.
2. **Acceso humano/app:** **C** sobre A — emitir URLs/credenciales de corta vida (read-only vs migrate vs admin) a partir del rol del project. No sustituye A. **Hecho:** `POST/GET/DELETE …/database/credentials`.
3. **Consola web pragmática (slice B ligero):** pgweb gestionado detrás de Authentik en path `/db-console` del host Atlas + `POST …/database/console-session` + botón **Open database**. No es proxy RLS in-Atlas; sí cumple “un click → SQL UI” sobre `apps` + schema del project. Upgrade path: CloudBeaver. Ver [db-console-hostname.md](../deployment/db-console-hostname.md).
4. **Diferir proxy+RLS in-process** — solo si pgweb/CloudBeaver no bastan.
5. **Secretos lógicos (slice inmediato, sin provisioning):** reutilizar el almacén de secrets existente:

   | Nombre lógico | Uso |
   |---------------|-----|
   | `db.url` | Connection string completa (preferido; suele mapear a `DATABASE_URL` en Compose) |
   | `db.schema` | Nombre de schema canónico del project (metadata; no secreto fuerte, pero binding útil) |
   | `db.password` | Solo si el repo parte host/user/db y no usa URL única (legacy / sketch ADR-0014) |

   Resolución: binding alias → project-owned → org/global (igual que `git.token`).
6. **Permisos por defecto (roles lógicos Atlas → grants Postgres, cuando exista provisioning):**

   | Perfil Atlas | Uso típico | Grants orientativos |
   |--------------|------------|---------------------|
   | `db.read` | Soporte, BI ligera | `CONNECT` + `USAGE` schema + `SELECT` |
   | `db.migrate` | CI / deploy / `migrateCommand` | + `CREATE`/`DDL` en schema propio + DML |
   | `db.admin` | Break-glass OPERATOR/ADMIN | + ownership schema / `CREATE` extensions solo si política lo permite |

   Default para runtime de app en deploy: **`db.migrate`** (la app crea/altera su schema). Default para reveal humano rutinario: **`db.read`**. `db.admin` auditado y raro.
7. **Convención de nombres (cluster compartido):** schema `app_<project_slug>` (slug Atlas, `[a-z0-9_]`). Rol app: `app_<project_slug>_migrator` (y opcional `_ro`). Si isolation = dedicated database: DB `app_<project_slug>`, schema `public` o el declarado en `db.schema`.
8. **Manifiesto:** `envFrom.secretRef: db.url` (o keys que el Compose espere). Atlas no interpreta SQL ni el ORM — solo entrega secret + opcionalmente corre `migrateCommand`.

## Fuera de alcance (este ADR)

- ~~Provisioner automático de roles/schemas en Postgres~~ — **hecho (slice 1):** `ATLAS_APP_DB_*` + `POST /projects/{id}/database/provision`.
- ~~Credenciales / URLs TTL (opción C)~~ — **hecho:** roles efímeros `VALID UNTIL`; perfiles `db.read` / `db.migrate` / `db.admin`; revoke + audit.
- ~~SQL console web (pgweb)~~ — **hecho (slice B ligero):** path `/db-console` + Open database; CloudBeaver / proxy+RLS in-process siguen opcionales.
- Backup/restore de DBs de customer apps (extender jobs más adelante; no mezclar con dump de Atlas).
- Cambiar Reelpath ni su login; otro agente puede estar en eso.

## Consecuencias

- (+) Encaja install single-tenant con muchos projects; aislamiento real sin multi-tenant SaaS en Atlas.
- (+) Reusa secrets + ADR-0014; cero dependencia de Prisma.
- (+) Deploy materializa `envFrom.secretRef` en `.env` (`db.url` → `DATABASE_URL`) sin loguear valores.
- (+) Menor riesgo ops que proxy+RLS in-Atlas: Postgres enforce isolation; consola es tool SSO-gated aparte.
- (+) TTL credentials no rotan el migrator/`db.url` (roles `app_<slug>_t_*` efímeros).
- (−) Manual sigue válido si `ATLAS_APP_DB_*` no está.
- → Producto: [project-database-access.md](../product/project-database-access.md). Consola: [db-console-hostname.md](../deployment/db-console-hostname.md).

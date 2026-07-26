# Módulos — Identity & access

## Users

**MVP:** username, password_hash, role (`ADMIN`|`OPERATOR`), SSO auto-provision.

**Objetivo:** email, display_name, authentik_uid, last_login, disabled flag, auth_provider (`LOCAL`|`AUTHENTIK`).

## Teams

Grupos locales para ACL de Projects. Membership User↔Team. Mapeo opcional desde grupos Authentik.

## Organizations

**Decisión:** una Organization implícita por instalación Atlas.

Tabla `organizations` (id, name, slug, settings JSON) con **una fila** sembrada. Sirve para:

- Settings globales tipados.
- Preparar `organization_id` FK en resources (nullable→NOT NULL tras backfill) sin multi-tenant SaaS.
- Futuro: varias orgs en una instalación (post-v1) sin reescritura total.

## Permissions

Permisos atómicos (`project.read`, `project.write`, `project.deploy`, `host.manage`, `secret.reveal`, `audit.read`, `billing.read`, `settings.write`).

Roles = sets de permisos. Roles built-in + custom (v1).

Evaluación: ADMIN bypass; else union de roles directos + teams.

## Audit

Append-only: actor, action, resource_type, resource_id, ip, user_agent, metadata JSON, timestamp.

UI filtrable; export. Retención configurable.

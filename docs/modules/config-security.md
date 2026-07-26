# Módulos — Configuración y secretos

## Variables

Pares key/value por Project/Service/Environment (`PRODUCTION`, `STAGING`, …). Inyectadas al deploy como env file generado por el worker.

Herencia: Organization defaults → Project → Service (override).

## Secrets

Igual que Variables pero cifrados; API nunca lista valores en claro. Endpoint `POST .../reveal` auditado y rate-limited (ADMIN/owner).

Rotación: crear nueva versión; deploys siguientes usan latest; keep N versions.

## Environments (concepto transversal)

No es un módulo sidebar obligatorio al inicio: flag/`environment` en Service o entidad `Environment` ligada a Project (v0.5+). Evita explosion de “projects” duplicados prod/staging.

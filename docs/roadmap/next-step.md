# Siguiente paso de implementación

## Estado del último incremento (completado)

**`composePath` opcional (ADR-0014 fase C / v0.8.10):**

- Create/update Project+Service: `composePath` ya no `@NotBlank`; DB `services.compose_path` nullable (Flyway V18).
- Deploy: `atlas.yml` `runtime.composeFile` gana; sin manifiesto → sintetiza manifiesto mínimo en memoria desde `composePath`; sin ambos → error claro.
- UI New/Edit Project: Runtime path opcional + hint `atlas.yml`.

**Previo:** lectura `atlas.yml` en deploy (fase B / v0.8.9); Cloudflare scopes; Auto-deploy; stale RUNNING; Proxmox REUSED; DNS CNAME; Tunnel PUBLIC.

## Recomendación única (siguiente)

**Fase D ligera (ADR-0014):** renombrar/ampliar mental model del port (`composeUp` → orquestación genérica) *sin* segundo runtime aún; o Host tags `runtime=compose` como prep. Alternativa producto: Host opcional en Pipeline (Autopilot en cada webhook).

## Por qué es el paso más rentable ahora

1. Fase C ya alinea contrato API/UI con manifiesto; falta desacoplar el port nombrado Compose.
2. Autopilot placement + edge ya maduros; no bloquean.
3. Segundo runtime (Podman/K8s) aún no; prep del port reduce deuda.

## Alcance concreto del incremento (siguiente)

1. Documentar / esbozar `RuntimeOrchestratorPort` (apply/teardown) sobre adapter Compose actual.
2. O: Pipeline sin `hostId` pin → Autopilot placement por run.
3. Tests + docs; **aún no** eliminar columna `compose_path`.

## Secundario (si sobra capacidad)

- UX Domains: mensaje explícito si Ensure falla por 403 (scopes insuficientes).
- OpenAPI / deprecations `/applications`.

## Norte estratégico (no es el siguiente incremento)

**Project manifest + runtime pluggable** ([ADR-0014](../decisions/ADR-0014-project-manifest-runtime.md)): fase D (port genérico, Host capacity tags). Compose sigue adapter default.

## Qué no hacer

- No billing/AI/marketplace, no Redis/Kafka obligatorio.
- No eliminar `composePath` de DB antes de migrar callers restantes.
- No `compose down -v` ni tocar `.env` en runbooks de deploy.

## Definición de éxito (siguiente)

> Deploy sigue verde con `composePath` opcional + `atlas.yml`; siguiente slice mueve el port hacia orquestación genérica *o* Autopilot por webhook sin host pin.

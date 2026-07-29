# UX — Flujos principales

## 1. Entrada (SSO)

```text
Abrir Atlas → GET /auth/sso
  ├─ 200: guardar JWT → Dashboard
  └─ 401: Login local (si habilitado) → POST /auth/login → Dashboard
```

## 2. Registrar y desplegar (camino feliz v0.4+)

```text
Projects → New Project
  → nombre, repo, branch, compose path, host, domain?
  → Create (persiste Project+Service)
  → Deploy now
      → Deployment PENDING/QUEUED
      → Worker RUNNING (logs live)
      → SUCCEEDED | FAILED
```

## 3. Deploy desde lista

```text
Deployments → New → elegir Service + Host → Create → mismo ciclo job
```

## 4. Gestión de host

```text
Hosts → New → hostname/IP + credencial (Secret)
  → Test connection (SYNC_HOST)
  → online=true, docker_version detectado
```

## 5. Secretos

```text
Secrets → Project detail → create owned or link org secret (alias)
Org secrets → ADMIN shared store
  → valor no se re-muestra
  → Reveal (confirm + audit) si permitido
```

## 6. Fallo de deploy (recuperación)

```text
Deployment FAILED → ver logs → Fix config/service → Redeploy
  → (opcional) Rollback a deployment SUCCEEDED anterior
```

## 7. Alerta

```text
Host offline event → Alert FIRING → Notification Slack
  → Operator abre Host → investigate → Sync
```

## 8. Webhook Git (v0.6+ / auto-deploy v0.8.7 / placement v0.8.12)

```text
Project → Enable auto-deploy
  → Pipeline (default, sin host pin) + webhook token
  → (opcional) GitHub API registra push webhook si hay git.token
  → Push a service.branch → POST /webhooks/git/{token}
  → filtro: solo push (ignora ping/PR/otras ramas)
  → PipelineRun → Autopilot placement (SHARED) → DEPLOY_SERVICE
```

Sin one-click: crear Pipeline manualmente (host Advanced opcional) y pegar URL/secret en GitHub (Settings → Webhooks).

## Permisos en flujos

| Flujo | ADMIN | OPERATOR | VIEWER |
|-------|-------|----------|--------|
| Deploy | ✓ | ✓ | ✗ |
| Manage hosts | ✓ | limitado | ✗ |
| Reveal secrets | ✓ | owner/ACL | ✗ |
| Settings | ✓ | ✗ | ✗ |
| Audit export | ✓ | ✗ | ✗ |

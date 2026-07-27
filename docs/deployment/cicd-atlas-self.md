# CI/CD — desplegar Atlas (producto) en producción

Dogfooding: cada push a `master` despliega **este** repositorio en la VM de producción.

Esto **no** es la feature in-app `DEPLOY_SERVICE` (pipelines de projects de cliente).

## Flujo

```text
push master / workflow_dispatch
  → GitHub Actions (runner self-hosted, labels: self-hosted,linux,atlas-prod)
  → SSH a la VM
  → /opt/atlas/atlas/scripts/deploy.sh (alias: `scripts/deploy-vm.sh`)
       git fetch + reset --hard origin/master
       (no toca .env ni docker-compose.prod.yml)
       docker compose up -d --build
       health: backend /actuator/health + frontend HTTP 2xx
```

El host de producción está en LAN privada (`192.168.x`). Los runners hospedados por GitHub **no** pueden alcanzarla; hace falta un **self-hosted runner** en la VM (o en la misma red).

## Secretos de GitHub

| Secret | Obligatorio | Ejemplo / notas |
|--------|-------------|-----------------|
| `ATLAS_DEPLOY_HOST` | sí | `192.168.1.35` |
| `ATLAS_DEPLOY_USER` | sí | `atlas` |
| `ATLAS_DEPLOY_SSH_KEY` | sí | Clave privada ed25519 (PEM completo) |
| `ATLAS_DEPLOY_PATH` | no | default `/opt/atlas/atlas` |
| `ATLAS_DEPLOY_KNOWN_HOSTS` | no | salida de `ssh-keyscan -H <host>` (recomendado) |


Alias opcionales (mismo valor; el workflow acepta cualquiera):

| Alias | Equivale a |
|-------|------------|
| `DEPLOY_HOST` | `ATLAS_DEPLOY_HOST` |
| `DEPLOY_USER` | `ATLAS_DEPLOY_USER` |
| `DEPLOY_SSH_KEY` | `ATLAS_DEPLOY_SSH_KEY` |
| `DEPLOY_SSH_PORT` | no usado (SSH al puerto 22; runner self-hosted en LAN) |

Clave pública en la VM (`~atlas/.ssh/authorized_keys`): comentario `github-actions-atlas-deploy` y/o `atlas-github-deploy`. No eliminar otras claves.

Configurar:

```bash
gh secret set ATLAS_DEPLOY_HOST -R AdriPardo/atlas -b '192.168.1.35'
gh secret set ATLAS_DEPLOY_USER -R AdriPardo/atlas -b 'atlas'
gh secret set ATLAS_DEPLOY_SSH_KEY -R AdriPardo/atlas < ~/.ssh/atlas_gha_deploy
ssh-keyscan -H 192.168.1.35 | gh secret set ATLAS_DEPLOY_KNOWN_HOSTS -R AdriPardo/atlas

# Equivalente con alias DEPLOY_* (opcionales si ya existen ATLAS_DEPLOY_*):
# gh secret set DEPLOY_HOST -R AdriPardo/atlas -b '192.168.1.35'
# gh secret set DEPLOY_USER -R AdriPardo/atlas -b 'atlas'
# gh secret set DEPLOY_SSH_KEY -R AdriPardo/atlas < ~/.ssh/atlas_gha_deploy
```

La clave pública correspondiente debe estar en `~atlas/.ssh/authorized_keys` en la VM.

## Bootstrap en la VM (una vez)

1. Checkout git en `/opt/atlas/atlas` con remote `origin` → este repo, rama `master`.
2. Copiar personalizaciones de compose a **`docker-compose.prod.yml`** (untracked; ver `.gitignore`). El script lo usa si existe y así sobrevive a `git reset --hard`.
3. Mantener **`.env`** solo en el host (nunca en git).
4. Asegurar que `scripts/deploy.sh` es ejecutable: `chmod +x scripts/deploy.sh`.
5. Instalar y registrar un GitHub Actions runner con labels `self-hosted,linux,atlas-prod` (ver abajo).
6. Probar: `ATLAS_APP_DIR=/opt/atlas/atlas ./scripts/deploy.sh`.

### Self-hosted runner (resumen)

```bash
# En la máquina con acceso a la LAN / en la propia VM, como usuario atlas:
mkdir -p ~/actions-runner && cd ~/actions-runner
# Descargar el tarball linux-x64 desde:
#   https://github.com/actions/runner/releases
# Registration token:
#   gh api -X POST repos/AdriPardo/atlas/actions/runners/registration-token --jq .token
./config.sh --url https://github.com/AdriPardo/atlas \
  --token <TOKEN> \
  --labels self-hosted,linux,atlas-prod \
  --name atlas-prod
sudo ./svc.sh install
sudo ./svc.sh start
```

Si `sudo` pide password interactivo, arrancar el runner en foreground/background como usuario:

```bash
cd ~/actions-runner && nohup ./run.sh > runner.log 2>&1 &
```

Para persistir tras reboot conviene instalar el servicio systemd (`svc.sh`) cuando haya sudo.

## Qué preserva el deploy

| Archivo | Tratamiento |
|---------|-------------|
| `.env` | Untracked — no lo toca `git reset` |
| `docker-compose.prod.yml` | Untracked — compose de producción |
| `docker-compose.override.yml` | Untracked — alternativa al prod file |
| Volúmenes Docker | Nunca `compose down -v` |

## Health checks

Si el frontend de producción publica un puerto host distinto de `3000` (p.ej. `3100:80` en `docker-compose.prod.yml`), define en el `.env` de la VM:

```bash
ATLAS_HEALTH_FRONTEND_URL=http://127.0.0.1:3100/
```

El Action **falla** si tras el build/up:

- Backend no responde `UP` en `http://127.0.0.1:8080/actuator/health`
- Frontend no responde HTTP 2xx en `ATLAS_HEALTH_FRONTEND_URL` (default `http://127.0.0.1:3000/`)

Reintentos: ~3 minutos (`ATLAS_HEALTH_RETRIES` × `ATLAS_HEALTH_SLEEP_SECS`).

## Rollback

En la VM:

```bash
cd /opt/atlas/atlas
git log --oneline -5
git reset --hard <sha_bueno>
# si usas prod file:
docker compose -f docker-compose.prod.yml up -d --build
# si no:
docker compose up -d --build
```

O volver a desplegar un commit anterior con revert en `master` / `workflow_dispatch` tras fix.

## Workflow

Archivo: [`.github/workflows/deploy-production.yml`](../../.github/workflows/deploy-production.yml)

- Concurrency: un deploy a la vez (`cancel-in-progress: false`)
- Timeout: 45 minutos
- Triggers: `push` a `master`, `workflow_dispatch`

Si faltan secretos, el job falla al inicio con un mensaje claro (no despliega a medias).

## Nota runner en la misma VM

El workflow escribe la clave y `UserKnownHostsFile` en rutas dedicadas (`~/.ssh/atlas_deploy*`) y **no** sobrescribe `~/.ssh/known_hosts`, para no romper `git fetch` hacia GitHub cuando el runner corre como el mismo usuario `atlas`.

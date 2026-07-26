# Atlas dashboard generator

Generates Grafana dashboards as code with the **Grafana Foundation SDK** and writes provisionable JSON under `monitoring/dashboards/generated/`.

## Technical decision

| Option            | Result                                                                                                                      |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------- |
| Language          | **TypeScript**                                                                                                              |
| SDK               | `@grafana/grafana-foundation-sdk@0.0.18`                                                                                    |
| Why not Go        | TypeScript is officially supported for Grafana ≥ 12, has published npm types/builders, and matches local macOS development. |
| Why not Grafonnet | Explicitly out of scope.                                                                                                    |
| Server runtime    | **No Node.js on the server.** Only the generated JSON is copied to Grafana provisioning.                                    |

SDK maturity is “public preview”, but it is the official Grafana Labs path for dashboards-as-code and is used here with APIs verified against installed type definitions (stat/table/logs/text/prometheus/loki/dashboard).

`schemaVersion` is forced to **41** after `build()` because the SDK default is currently 42.

## Repository layout

```text
monitoring/
  dashboard-generator/          # this project (dev machine only)
  dashboards/
    official/                   # untouched official dashboards (empty in git today)
    generated/
      atlas-operations-center.json
```

Server provisioning path (operator-provided):

`/opt/atlas/infrastructure/monitoring/dashboards`

## Prerequisites (local / macOS)

- Node.js ≥ 20
- npm

## Install

```bash
cd monitoring/dashboard-generator
npm install
```

## Generate

```bash
make generate
# or
npm run generate
```

Writes:

`monitoring/dashboards/generated/atlas-operations-center.json`

Fails non-zero if JSON is invalid or grid validation fails.

## Validate

```bash
make test
make lint
make typecheck
make format-check
```

Or all:

```bash
make all
```

## Copy to server (manual)

Do **not** run this from CI/cloud blindly. From a machine that can reach the host:

```bash
scp monitoring/dashboards/generated/atlas-operations-center.json \
  atlas@192.168.1.35:/opt/atlas/infrastructure/monitoring/dashboards/generated/atlas-operations-center.json
```

Server-side checks:

```bash
python3 -m json.tool \
  /opt/atlas/infrastructure/monitoring/dashboards/generated/atlas-operations-center.json \
  >/dev/null && echo "JSON válido"

docker logs grafana --since 2m | grep -Ei \
  "provision|dashboard|error|failed"
```

In Grafana UI: Dashboards → search **Atlas Operations Center** (`uid: atlas-operations-center`).

## Pending configuration

### Quick link URLs

File: `src/config/urls.ts`

Only GitHub is hard-coded (verified repo URL). Homepage, Prometheus, Alertmanager, Grafana, Loki, Traefik, and Proxmox are intentionally empty until hostnames exist in repo/provisioning evidence.

### Loki stream selector

File: `src/config/loki.ts`

Alloy/Loki label schemas were **not found** in this repository. The panel uses:

```logql
{job=~".+"} |~ "(?i)error|warn|warning|fatal|panic"
```

Manual check in Grafana Explore (Loki datasource `uid: loki`):

1. Run `{job=~".+"}` and inspect real labels (`service_name`, `container`, `compose_service`, …).
2. Update `LOKI_STREAM_SELECTOR`.
3. Re-run `make generate` and redeploy the JSON.

### Official dashboard links

`SERVICE_DASHBOARD_UIDS` in `src/config/urls.ts` is empty because `monitoring/dashboards/official/` has no JSON yet. Add UIDs only after real files exist.

### Last Backup

Shown as **NOT CONFIGURED** (text panel). No fake Prometheus metric.

## Adding panels

1. Prefer a reusable component under `src/components/`.
2. Wire it in `src/dashboards/atlasOperationsCenter.ts`.
3. Keep `span` totals ≤ 24 per row; use `height`/`span` from the SDK builders.
4. Run `make generate && make test`.

## Architecture

```text
src/main.ts
  → dashboards/atlasOperationsCenter.ts
      → components/* (stat, resource, table, logs, links)
      → config/{datasources,thresholds,urls,loki}.ts
  → serialize.ts (schemaVersion 41, strip ids, grid checks)
  → ../dashboards/generated/*.json
```

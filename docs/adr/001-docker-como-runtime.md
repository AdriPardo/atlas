# ADR-001 — Docker como runtime

## Estado

Propuesta / **no verificada en host** (2026-07-26)

## Contexto

El briefing de plataforma asume contenedores Docker Compose (Traefik, Prometheus, Grafana, etc.). En el repositorio Git **no hay** compose ni evidencias de runtime.

## Decisión (declarada, pendiente de validación)

Usar Docker (Compose) como runtime estándar de servicios Atlas.

## Alternativas (no evaluadas con evidencia local)

- Podman / Quadlet
- Kubernetes (k3s/k8s)
- Binarios systemd nativos

## Por qué (razonamiento de plataforma, no evidencia de despliegue)

- Empaquetado reproducible por servicio
- Aislamiento de redes/volúmenes
- Ecosistema amplio de imágenes oficiales para observabilidad

## Consecuencias

- Requiere disciplina de tags/digests, redes y backups de volúmenes
- Debe validarse versión de Engine/Compose en el host

## Evidencia requerida para aceptar

Salida de `docker version`, compose files y contenedores en ejecución.

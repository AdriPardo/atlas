/** Public Atlas hostname(s) protected by Traefik + Authentik ForwardAuth. */
const PUBLIC_HOSTS = new Set(['atlas.atlasops.dev'])

export const PUBLIC_ATLAS_URL = 'https://atlas.atlasops.dev/'

export function isAtlasPublicHost(hostname = window.location.hostname): boolean {
  return PUBLIC_HOSTS.has(hostname)
}

/** Localhost / loopback / raw LAN IP — no Traefik ForwardAuth on this URL. */
export function isDirectAccessHost(hostname = window.location.hostname): boolean {
  if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '[::1]' || hostname === '::1') {
    return true
  }
  return /^\d{1,3}(?:\.\d{1,3}){3}$/.test(hostname)
}

/** Prefer local username/password only off the public Authentik edge. */
export function allowLocalLogin(hostname = window.location.hostname): boolean {
  return !isAtlasPublicHost(hostname)
}

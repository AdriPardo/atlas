/** Public Atlas hostname(s) protected by Traefik + Authentik ForwardAuth. */
const PUBLIC_HOSTS = new Set(['atlas.atlasops.dev'])

export const PUBLIC_ATLAS_URL = 'https://atlas.atlasops.dev/'

/** Authentik embedded outpost — triggers ForwardAuth sign-in (full navigation). */
export const AUTHENTIK_OUTPOST_START_PATH = '/outpost.goauthentik.io/start'

/** Authentik embedded outpost — ends ForwardAuth session. */
export const AUTHENTIK_OUTPOST_SIGN_OUT_PATH = '/outpost.goauthentik.io/sign_out'

/** URL that sends the browser through Traefik ForwardAuth → Authentik login. */
export function authentikSignInUrl(returnTo = `${window.location.origin}/`): string {
  const target = returnTo.startsWith('http') ? returnTo : `${window.location.origin}${returnTo}`
  return `${window.location.origin}${AUTHENTIK_OUTPOST_START_PATH}?rd=${encodeURIComponent(target)}`
}

/** Full-page redirect to Authentik (never rely on client-side /login for prod SSO). */
export function redirectToAuthentikSignIn(returnTo?: string): void {
  window.location.assign(authentikSignInUrl(returnTo))
}

/** Clears Authentik proxy session and returns to Atlas (or login). */
export function redirectToAuthentikSignOut(returnTo = `${window.location.origin}/`): void {
  const target = returnTo.startsWith('http') ? returnTo : `${window.location.origin}${returnTo}`
  window.location.assign(
    `${window.location.origin}${AUTHENTIK_OUTPOST_SIGN_OUT_PATH}?rd=${encodeURIComponent(target)}`,
  )
}

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

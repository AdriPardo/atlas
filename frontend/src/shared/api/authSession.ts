import { tokenStorage } from './tokenStorage'

/** Full-page SSO bootstrap — ForwardAuth headers only on document navigation. */
export const SSO_BOOTSTRAP_PATH = '/api/v1/auth/sso/bootstrap'

const SSO_REDIRECT_KEY = 'atlas.sso.redirect'
const SSO_ERROR_KEY = 'atlas.sso.error'
const SSO_REDIRECT_TTL_MS = 60_000

export type SsoFailureCode =
  | 'redirect_blocked'
  | 'token_rejected'
  | 'sso_disabled'
  | 'identity_missing'
  | 'mint_failed'

function isPublicAuthPath(url: string): boolean {
  return url.includes('/auth/sso') || url.includes('/auth/login')
}

export function buildSsoBootstrapUrl(returnTo: string): string {
  return `${SSO_BOOTSTRAP_PATH}?returnTo=${encodeURIComponent(returnTo)}`
}

export function clearSsoRedirectFlag(): void {
  sessionStorage.removeItem(SSO_REDIRECT_KEY)
}

export function setSsoError(code: SsoFailureCode): void {
  sessionStorage.setItem(SSO_ERROR_KEY, code)
}

export function consumeSsoError(): SsoFailureCode | null {
  const code = sessionStorage.getItem(SSO_ERROR_KEY)
  sessionStorage.removeItem(SSO_ERROR_KEY)
  if (!code) return null
  return code as SsoFailureCode
}

export function ssoErrorMessage(code: SsoFailureCode): string {
  switch (code) {
    case 'redirect_blocked':
      return 'El flujo SSO se interrumpió antes de guardar el token. Pulsa reintentar para volver a Authentik.'
    case 'token_rejected':
      return 'Atlas rechazó la sesión tras el login (JWT inválido o usuario no encontrado en la base de datos).'
    case 'sso_disabled':
      return 'SSO Authentik está desactivado en el servidor Atlas.'
    case 'identity_missing':
      return 'Authentik no inyectó cabeceras X-authentik-* en el bootstrap. Revisa ForwardAuth en Traefik.'
    case 'mint_failed':
      return 'No se pudo generar el token de sesión tras el login Authentik.'
    default:
      return 'Error de inicio de sesión SSO.'
  }
}

/** True when we already sent the browser to SSO bootstrap recently (break redirect loops). */
export function isSsoRedirectInFlight(): boolean {
  const raw = sessionStorage.getItem(SSO_REDIRECT_KEY)
  if (!raw) return false
  const started = Number(raw)
  if (!Number.isFinite(started) || Date.now() - started > SSO_REDIRECT_TTL_MS) {
    sessionStorage.removeItem(SSO_REDIRECT_KEY)
    return false
  }
  return true
}

/**
 * One-shot full-page SSO bootstrap. Traefik router `atlas-sso-bootstrap` runs ForwardAuth
 * so the backend receives X-authentik-* and mints JWT into localStorage.
 *
 * @returns false when a redirect is already in flight (caller must not retry).
 */
export function redirectToSsoBootstrap(returnTo?: string): boolean {
  if (isSsoRedirectInFlight()) {
    return false
  }

  const path =
    returnTo && returnTo.startsWith('/')
      ? returnTo
      : `${window.location.pathname}${window.location.search}`

  sessionStorage.removeItem(SSO_ERROR_KEY)
  sessionStorage.setItem(SSO_REDIRECT_KEY, String(Date.now()))
  window.location.replace(buildSsoBootstrapUrl(path))
  return true
}

/**
 * Prod: JWT only via full-page bootstrap (XHR lacks ForwardAuth headers on mint path).
 * Dev: return stored local token.
 */
export async function refreshAuthToken(): Promise<string | null> {
  return tokenStorage.get()
}

export function hasAuthToken(): boolean {
  return !!tokenStorage.get()
}

export { isPublicAuthPath }

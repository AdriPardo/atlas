import { tokenStorage } from './tokenStorage'

/** Full-page SSO bootstrap — ForwardAuth headers only on document navigation. */
export const SSO_BOOTSTRAP_PATH = '/api/v1/auth/sso/bootstrap'

const SSO_REDIRECT_KEY = 'atlas.sso.redirect'
const SSO_ERROR_KEY = 'atlas.sso.error'
/** Prevents double navigation to bootstrap within the same page load only. */
const SSO_REDIRECT_TTL_MS = 2_000

export type SsoFailureCode =
  | 'redirect_blocked'
  | 'token_rejected'
  | 'jwt_invalid'
  | 'user_not_found'
  | 'sso_disabled'
  | 'identity_missing'
  | 'mint_failed'

function isPublicAuthPath(url: string): boolean {
  return url.includes('/auth/sso') || url.includes('/auth/login')
}

export function buildSsoBootstrapUrl(returnTo: string): string {
  return `${SSO_BOOTSTRAP_PATH}?returnTo=${encodeURIComponent(returnTo)}`
}

export function buildSsoBootstrapAbsoluteUrl(returnTo: string): string {
  return `${window.location.origin}${buildSsoBootstrapUrl(returnTo)}`
}

export function clearSsoRedirectFlag(): void {
  sessionStorage.removeItem(SSO_REDIRECT_KEY)
}

/** @deprecated attempt guard removed — kept for callers clearing session state */
export function clearSsoAttempt(): void {
  clearSsoRedirectFlag()
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

export function isRecoverableSsoFailure(code: SsoFailureCode): boolean {
  return (
    code === 'token_rejected' ||
    code === 'jwt_invalid' ||
    code === 'identity_missing' ||
    code === 'mint_failed'
  )
}

export function isTerminalSsoFailure(code: SsoFailureCode): boolean {
  return code === 'sso_disabled' || code === 'user_not_found'
}

export function shouldShowSsoError(code: SsoFailureCode): boolean {
  return isTerminalSsoFailure(code) || isRecoverableSsoFailure(code)
}

export function mapMeFailureStatus(status?: number): SsoFailureCode {
  if (status === 404) return 'user_not_found'
  if (status === 403) return 'jwt_invalid'
  return 'token_rejected'
}

export function ssoErrorMessage(code: SsoFailureCode): string {
  switch (code) {
    case 'redirect_blocked':
      return 'No se pudo iniciar el bootstrap SSO. Pulsa reintentar.'
    case 'jwt_invalid':
      return 'Atlas rechazó el JWT de sesión. Pulsa reintentar para obtener uno nuevo.'
    case 'user_not_found':
      return 'Tu cuenta Authentik no está provisionada en Atlas. Contacta al administrador.'
    case 'token_rejected':
      return 'Atlas rechazó la sesión tras el bootstrap. Pulsa reintentar.'
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

/** True only while bootstrap navigation was just triggered (same page, anti double-click). */
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

/** Bootstrap succeeded — hash or cookie handoff clears any in-flight guard. */
export function markBootstrapReturn(): void {
  clearSsoRedirectFlag()
  sessionStorage.removeItem(SSO_ERROR_KEY)
}

/**
 * Full-page GET bootstrap. Traefik ForwardAuth injects X-authentik-* on document nav.
 * Returning from bootstrap (JWT in hash/cookie) clears the in-flight guard automatically.
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
  window.location.replace(buildSsoBootstrapAbsoluteUrl(path))
  return true
}

/** Manual retry — full-page bootstrap, always allowed. */
export function forceSsoReauth(returnTo?: string): void {
  tokenStorage.clear()
  clearSsoRedirectFlag()
  sessionStorage.removeItem(SSO_ERROR_KEY)
  redirectToSsoBootstrap(returnTo)
}

export async function refreshAuthToken(): Promise<string | null> {
  return tokenStorage.get()
}

export function hasAuthToken(): boolean {
  return !!tokenStorage.get()
}

export { isPublicAuthPath }

import { tokenStorage } from './tokenStorage'

/** Full-page SSO bootstrap — ForwardAuth headers only on document navigation. */
export const SSO_BOOTSTRAP_PATH = '/api/v1/auth/sso/bootstrap'

const SSO_REDIRECT_KEY = 'atlas.sso.redirect'
const SSO_ERROR_KEY = 'atlas.sso.error'
/** One bootstrap navigation per browser tab session — breaks redirect storms. */
const SSO_ATTEMPT_KEY = 'atlas.sso.attempt'
const SSO_REDIRECT_TTL_MS = 5_000

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

export function clearSsoAttempt(): void {
  sessionStorage.removeItem(SSO_ATTEMPT_KEY)
}

export function hasExhaustedSsoAttempts(): boolean {
  return sessionStorage.getItem(SSO_ATTEMPT_KEY) === '1'
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

/** Show error UI — never auto-redirect again until user clicks retry. */
export function isRecoverableSsoFailure(code: SsoFailureCode): boolean {
  return (
    code === 'redirect_blocked' ||
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
      return 'El flujo SSO ya se intentó en esta pestaña. Pulsa reintentar o cierra sesión en Authentik.'
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
 * Full-page GET bootstrap (Traefik ForwardAuth injects X-authentik-*).
 * SPA load implies Authentik session — go direct to bootstrap, NOT outpost again.
 * Max one attempt per tab until user clicks retry or logs out.
 */
export function redirectToSsoBootstrap(returnTo?: string): boolean {
  if (isSsoRedirectInFlight()) {
    return false
  }
  if (hasExhaustedSsoAttempts()) {
    return false
  }

  const path =
    returnTo && returnTo.startsWith('/')
      ? returnTo
      : `${window.location.pathname}${window.location.search}`

  sessionStorage.removeItem(SSO_ERROR_KEY)
  sessionStorage.setItem(SSO_ATTEMPT_KEY, '1')
  sessionStorage.setItem(SSO_REDIRECT_KEY, String(Date.now()))
  window.location.replace(buildSsoBootstrapAbsoluteUrl(path))
  return true
}

/** Manual retry — clears attempt counter and navigates to bootstrap once. */
export function forceSsoReauth(returnTo?: string): void {
  tokenStorage.clear()
  clearSsoRedirectFlag()
  clearSsoAttempt()
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

import { authentikSignInUrl } from '../../features/auth/authHost'
import { tokenStorage } from './tokenStorage'

/** Full-page SSO bootstrap — ForwardAuth headers only on document navigation. */
export const SSO_BOOTSTRAP_PATH = '/api/v1/auth/sso/bootstrap'

const SSO_REDIRECT_KEY = 'atlas.sso.redirect'
const SSO_ERROR_KEY = 'atlas.sso.error'
/** Short TTL — only prevents double-redirect within the same tick, not recovery after stale JWT. */
const SSO_REDIRECT_TTL_MS = 3_000

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

export function setSsoError(code: SsoFailureCode): void {
  sessionStorage.setItem(SSO_ERROR_KEY, code)
}

export function consumeSsoError(): SsoFailureCode | null {
  const code = sessionStorage.getItem(SSO_ERROR_KEY)
  sessionStorage.removeItem(SSO_ERROR_KEY)
  if (!code) return null
  return code as SsoFailureCode
}

/** Errors that require admin/server fix — do not loop SSO redirects. */
export function isTerminalSsoFailure(code: SsoFailureCode): boolean {
  return code === 'sso_disabled' || code === 'user_not_found'
}

export function mapMeFailureStatus(status?: number): SsoFailureCode {
  if (status === 404) return 'user_not_found'
  if (status === 403) return 'jwt_invalid'
  return 'token_rejected'
}

export function ssoErrorMessage(code: SsoFailureCode): string {
  switch (code) {
    case 'redirect_blocked':
      return 'No se pudo iniciar el flujo SSO. Pulsa reintentar.'
    case 'jwt_invalid':
      return 'Sesión caducada. Redirigiendo a Authentik…'
    case 'user_not_found':
      return 'Tu cuenta Authentik no está provisionada en Atlas. Contacta al administrador.'
    case 'token_rejected':
      return 'Sesión rechazada. Redirigiendo a Authentik…'
    case 'sso_disabled':
      return 'SSO Authentik está desactivado en el servidor Atlas.'
    case 'identity_missing':
      return 'Authentik no inyectó cabeceras en el bootstrap. Revisa ForwardAuth en Traefik.'
    case 'mint_failed':
      return 'No se pudo generar el token de sesión tras el login Authentik.'
    default:
      return 'Error de inicio de sesión SSO.'
  }
}

/** True when we redirected to SSO within the last few seconds (same navigation only). */
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
 * Full-page Authentik login → SSO bootstrap → JWT cookie → SPA.
 * Never hit bootstrap directly: ForwardAuth must run on outpost first.
 *
 * @returns false only when a redirect was fired within the last few ms (same tick).
 */
export function redirectToSsoBootstrap(returnTo?: string): boolean {
  if (isSsoRedirectInFlight()) {
    return false
  }

  const path =
    returnTo && returnTo.startsWith('/')
      ? returnTo
      : `${window.location.pathname}${window.location.search}`

  const bootstrapUrl = buildSsoBootstrapAbsoluteUrl(path)

  sessionStorage.removeItem(SSO_ERROR_KEY)
  sessionStorage.setItem(SSO_REDIRECT_KEY, String(Date.now()))
  window.location.replace(authentikSignInUrl(bootstrapUrl))
  return true
}

/** Clear Atlas JWT and force a fresh Authentik + bootstrap cycle. */
export function forceSsoReauth(returnTo?: string): void {
  tokenStorage.clear()
  clearSsoRedirectFlag()
  sessionStorage.removeItem(SSO_ERROR_KEY)
  redirectToSsoBootstrap(returnTo)
}

/** Prod: JWT via bootstrap cookie or localStorage. Dev: stored local token. */
export async function refreshAuthToken(): Promise<string | null> {
  return tokenStorage.get()
}

export function hasAuthToken(): boolean {
  return !!tokenStorage.get()
}

export { isPublicAuthPath }

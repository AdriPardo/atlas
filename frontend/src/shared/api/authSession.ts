import {
  AUTHENTIK_OUTPOST_START_PATH,
  isAtlasPublicHost,
} from '../../features/auth/authHost'
import { tokenStorage } from './tokenStorage'

/** Full-page SSO bootstrap — ForwardAuth headers only on document navigation. */
export const SSO_BOOTSTRAP_PATH = '/api/v1/auth/sso/bootstrap'

function isPublicAuthPath(url: string): boolean {
  return url.includes('/auth/sso') || url.includes('/auth/login')
}

export function buildSsoBootstrapUrl(returnTo: string): string {
  return `${SSO_BOOTSTRAP_PATH}?returnTo=${encodeURIComponent(returnTo)}`
}

/**
 * Navigate to SSO bootstrap. On the public Authentik edge, go through the embedded
 * outpost first so ForwardAuth never returns 403 for an unauthenticated bootstrap GET.
 * After login, Authentik returns to bootstrap with X-authentik-* headers and the API
 * mints the JWT into localStorage.
 */
export function redirectToSsoBootstrap(returnTo?: string): void {
  const path =
    returnTo && returnTo.startsWith('/')
      ? returnTo
      : `${window.location.pathname}${window.location.search}`

  if (isAtlasPublicHost()) {
    const bootstrapUrl = `${window.location.origin}${buildSsoBootstrapUrl(path)}`
    window.location.assign(
      `${AUTHENTIK_OUTPOST_START_PATH}?rd=${encodeURIComponent(bootstrapUrl)}`,
    )
    return
  }

  window.location.assign(buildSsoBootstrapUrl(path))
}

/**
 * Prod: JWT only via full-page bootstrap (XHR /auth/sso lacks ForwardAuth headers).
 * Dev: return stored local token.
 */
export async function refreshAuthToken(): Promise<string | null> {
  if (isAtlasPublicHost()) {
    const token = tokenStorage.get()
    if (token) return token
    redirectToSsoBootstrap()
    return new Promise(() => {})
  }
  return tokenStorage.get()
}

export function hasAuthToken(): boolean {
  return !!tokenStorage.get()
}

export { isPublicAuthPath }

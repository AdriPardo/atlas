import { isAtlasPublicHost } from '../../features/auth/authHost'
import { tokenStorage } from './tokenStorage'

/** Full-page SSO bootstrap — ForwardAuth headers only on document navigation. */
export const SSO_BOOTSTRAP_PATH = '/api/v1/auth/sso/bootstrap'

function isPublicAuthPath(url: string): boolean {
  return url.includes('/auth/sso') || url.includes('/auth/login')
}

/** Navigate to SSO bootstrap (or Authentik outpost via backend redirect). */
export function redirectToSsoBootstrap(returnTo?: string): void {
  const path =
    returnTo && returnTo.startsWith('/')
      ? returnTo
      : `${window.location.pathname}${window.location.search}`
  window.location.assign(`${SSO_BOOTSTRAP_PATH}?returnTo=${encodeURIComponent(path)}`)
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

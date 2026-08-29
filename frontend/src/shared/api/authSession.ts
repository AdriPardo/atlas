import axios from 'axios'
import { isAtlasPublicHost } from '../../features/auth/authHost'
import { tokenStorage } from './tokenStorage'

/** Bare client for SSO — avoids axios auth-interceptor loops. */
export const ssoClient = axios.create({ baseURL: '/api/v1' })

let refreshPromise: Promise<string | null> | null = null

async function sleep(ms: number) {
  await new Promise((resolve) => setTimeout(resolve, ms))
}

function isPublicAuthPath(url: string): boolean {
  return url.includes('/auth/sso') || url.includes('/auth/login')
}

/**
 * Mint a fresh Atlas JWT from Authentik ForwardAuth headers (prod) or keep local token.
 * Concurrent callers share one in-flight refresh (StrictMode / parallel 401s).
 */
export async function refreshAuthToken(attempts = 3): Promise<string | null> {
  if (refreshPromise) {
    return refreshPromise
  }

  refreshPromise = (async () => {
    if (isAtlasPublicHost()) {
      for (let i = 0; i < attempts; i += 1) {
        try {
          const { data } = await ssoClient.get<{ accessToken: string }>('/auth/sso')
          if (data.accessToken) {
            tokenStorage.set(data.accessToken)
            return data.accessToken
          }
        } catch {
          if (i < attempts - 1) {
            await sleep(300 * (i + 1))
          }
        }
      }
      return null
    }
    return tokenStorage.get()
  })().finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}

export function hasAuthToken(): boolean {
  return !!tokenStorage.get()
}

export { isPublicAuthPath }

import { api, tokenStorage, SKIP_AUTH_RETRY_HEADER } from './client'

let refreshInFlight: Promise<boolean> | null = null

const skipAuthRetry = { headers: { [SKIP_AUTH_RETRY_HEADER]: '1' } }

/** Mint or verify Atlas JWT (SSO ForwardAuth or stored token + /me). */
export async function refreshAtlasSession(): Promise<boolean> {
  if (refreshInFlight) {
    return refreshInFlight
  }

  refreshInFlight = (async () => {
    const existing = tokenStorage.get()
    if (existing) {
      try {
        await api.get('/me', skipAuthRetry)
        return true
      } catch {
        tokenStorage.clear()
      }
    }

    try {
      const mint = await api
        .get<{ accessToken: string }>('/auth/sso', skipAuthRetry)
        .then((r) => r.data)
      tokenStorage.set(mint.accessToken)
      await api.get('/me', skipAuthRetry)
      return true
    } catch {
      tokenStorage.clear()
      return false
    }
  })()

  try {
    return await refreshInFlight
  } finally {
    refreshInFlight = null
  }
}

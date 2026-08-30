import { api, tokenStorage, SKIP_AUTH_RETRY_HEADER } from './client'
import type { User } from '../types/api'

const skipAuthRetry = { headers: { [SKIP_AUTH_RETRY_HEADER]: '1' } }

/** Serializes SSO mint + /me probe — prevents parallel /auth/sso races. */
let sessionLock: Promise<User | null> | null = null

async function sleep(ms: number) {
  await new Promise((resolve) => setTimeout(resolve, ms))
}

async function probeSessionOnce(): Promise<User | null> {
  const existing = tokenStorage.get()
  if (existing) {
    try {
      const { data } = await api.get<User>('/me', skipAuthRetry)
      return data
    } catch {
      tokenStorage.clear()
    }
  }

  try {
    const mint = await api.get<{ accessToken: string }>('/auth/sso', skipAuthRetry)
    tokenStorage.set(mint.data.accessToken)
    const { data } = await api.get<User>('/me', skipAuthRetry)
    return data
  } catch {
    tokenStorage.clear()
    return null
  }
}

/** Bootstrap or refresh Atlas session (SSO mint → store JWT → verify /me). */
export async function bootstrapSession(attempts = 1): Promise<User | null> {
  if (sessionLock) {
    return sessionLock
  }

  sessionLock = (async () => {
    for (let i = 0; i < attempts; i += 1) {
      const user = await probeSessionOnce()
      if (user && tokenStorage.get()) {
        return user
      }
      if (i < attempts - 1) {
        await sleep(350 * (i + 1))
      }
    }
    return null
  })()

  try {
    return await sessionLock
  } finally {
    sessionLock = null
  }
}

/** Used by axios interceptor after 401/403. */
export async function refreshAtlasSession(): Promise<boolean> {
  const user = await bootstrapSession(1)
  return !!user && !!tokenStorage.get()
}

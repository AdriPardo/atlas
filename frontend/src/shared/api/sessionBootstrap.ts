import axios from 'axios'
import { api, SKIP_AUTH_RETRY_HEADER } from './client'
import { tokenStorage } from './tokenStorage'
import type { User } from '../types/api'

const skipAuthRetry = { headers: { [SKIP_AUTH_RETRY_HEADER]: '1' } }

/** Serializes SSO mint + /me probe — prevents parallel /auth/sso or /me races. */
let sessionLock: Promise<User | null> | null = null
let sessionResolved = false
let cachedUser: User | null = null

async function sleep(ms: number) {
  await new Promise((resolve) => setTimeout(resolve, ms))
}

function isUnauthorized(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 401
}

async function probeSessionOnce(): Promise<User | null> {
  const existing = tokenStorage.get()
  if (existing) {
    try {
      const { data } = await api.get<User>('/me', skipAuthRetry)
      return data
    } catch (error) {
      // Only drop JWT on definitive 401 — 403 may be ForwardAuth timing, keep token.
      if (isUnauthorized(error)) {
        tokenStorage.clear()
      }
    }
  }

  try {
    const mint = await api.get<{ accessToken: string }>('/auth/sso', skipAuthRetry)
    tokenStorage.set(mint.data.accessToken)
    const { data } = await api.get<User>('/me', skipAuthRetry)
    return data
  } catch {
    return null
  }
}

/** Bootstrap or refresh Atlas session (SSO mint → store JWT → verify /me). Deduped globally. */
export async function bootstrapSession(attempts = 1): Promise<User | null> {
  if (sessionResolved && cachedUser && tokenStorage.get()) {
    return cachedUser
  }

  if (sessionLock) {
    return sessionLock
  }

  sessionLock = (async () => {
    for (let i = 0; i < attempts; i += 1) {
      const user = await probeSessionOnce()
      if (user && tokenStorage.get()) {
        cachedUser = user
        sessionResolved = true
        return user
      }
      if (i < attempts - 1) {
        await sleep(350 * (i + 1))
      }
    }
    sessionResolved = false
    cachedUser = null
    return null
  })()

  try {
    return await sessionLock
  } finally {
    sessionLock = null
  }
}

/** Used by axios interceptor after 401. */
export async function refreshAtlasSession(): Promise<boolean> {
  sessionResolved = false
  cachedUser = null
  const user = await bootstrapSession(1)
  return !!user && !!tokenStorage.get()
}

export function resetSessionCache(): void {
  sessionResolved = false
  cachedUser = null
}

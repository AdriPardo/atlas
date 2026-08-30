import { api, BOOTSTRAP_HEADER } from './client'
import { tokenStorage } from './tokenStorage'
import type { User } from '../types/api'
import { isAtlasPublicHost } from '../../features/auth/authHost'

const bootstrapOpts = { headers: { [BOOTSTRAP_HEADER]: '1' } }

let sessionUser: User | null = null
let sessionReady = false
let bootPromise: Promise<User | null> | null = null

async function sleep(ms: number) {
  await new Promise((resolve) => setTimeout(resolve, ms))
}

export function getSessionUser(): User | null {
  return sessionUser
}

export function isSessionReady(): boolean {
  return sessionReady && !!tokenStorage.get() && !!sessionUser
}

/**
 * Exactly-once session bootstrap per page load (deduped module singleton).
 * Flow: reuse stored JWT → single GET /me, OR GET /auth/sso → store JWT → single GET /me.
 */
export async function bootstrapAuthSession(): Promise<User | null> {
  if (isSessionReady()) {
    return sessionUser
  }
  if (bootPromise) {
    return bootPromise
  }

  bootPromise = (async () => {
    const attempts = isAtlasPublicHost() ? 4 : 2

    for (let i = 0; i < attempts; i += 1) {
      const existing = tokenStorage.get()
      if (existing) {
        try {
          const { data } = await api.get<User>('/me', bootstrapOpts)
          sessionUser = data
          sessionReady = true
          return data
        } catch {
          tokenStorage.clear()
          sessionUser = null
          sessionReady = false
        }
      }

      try {
        const mint = await api.get<{ accessToken: string }>('/auth/sso', bootstrapOpts)
        tokenStorage.set(mint.data.accessToken)
        const { data } = await api.get<User>('/me', bootstrapOpts)
        sessionUser = data
        sessionReady = true
        return data
      } catch {
        if (i < attempts - 1) {
          await sleep(350 * (i + 1))
        }
      }
    }

    sessionUser = null
    sessionReady = false
    return null
  })()

  try {
    return await bootPromise
  } finally {
    bootPromise = null
  }
}

/** After password login — one /me with new token. */
export async function activateSessionToken(token: string): Promise<User | null> {
  tokenStorage.set(token)
  sessionUser = null
  sessionReady = false
  bootPromise = null
  return bootstrapAuthSession()
}

export function resetAuthSession(): void {
  sessionUser = null
  sessionReady = false
  bootPromise = null
  tokenStorage.clear()
}

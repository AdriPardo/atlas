import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { queryClient } from '../../app/queryClient'
import { refreshAuthToken } from '../../shared/api/authSession'
import { tokenStorage } from '../../shared/api/client'
import { meApi, authApi } from '../../shared/api/endpoints'
import type { User } from '../../shared/types/api'
import { isAtlasPublicHost } from './authHost'

interface AuthContextValue {
  user: User | null
  loading: boolean
  /** Bootstrap done and bearer token present — safe to fire API queries. */
  authReady: boolean
  /** True after bootstrap finished and Authentik SSO did not mint a session. */
  ssoFailed: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  refreshUser: () => Promise<void>
  /** Re-probe /auth/sso (e.g. after ForwardAuth cookie is ready). */
  retrySso: () => Promise<User | null>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

async function sleep(ms: number) {
  await new Promise((resolve) => setTimeout(resolve, ms))
}

async function tryAuthentikSso(): Promise<User | null> {
  try {
    if (isAtlasPublicHost()) {
      // ForwardAuth headers may authenticate /me even when Traefik strips Authorization.
      try {
        const profile = await meApi.get()
        void refreshAuthToken(1).catch(() => undefined)
        return profile
      } catch {
        /* mint JWT below */
      }
    }

    const result = await authApi.sso()
    if (result?.accessToken) {
      tokenStorage.set(result.accessToken)
    }
    return await meApi.get()
  } catch {
    return null
  }
}

/** Retry SSO — covers brief backend restarts / 502 while Traefik already authenticated. */
async function tryAuthentikSsoWithRetry(attempts = 3): Promise<User | null> {
  for (let i = 0; i < attempts; i += 1) {
    const user = await tryAuthentikSso()
    if (user) return user
    if (i < attempts - 1) {
      await sleep(350 * (i + 1))
    }
  }
  return null
}

async function settleAuthenticatedSession(user: User | null) {
  if (user && tokenStorage.get()) {
    await queryClient.invalidateQueries()
  }
  return user
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [ssoFailed, setSsoFailed] = useState(false)

  const refreshUser = useCallback(async () => {
    setLoading(true)
    setSsoFailed(false)
    try {
      // On the Authentik edge, always re-mint JWT so role claims match Authentik groups + DB.
      if (isAtlasPublicHost()) {
        const token = await refreshAuthToken(4)
        if (token) {
          try {
            const profile = await meApi.get()
            setUser(await settleAuthenticatedSession(profile))
            return
          } catch {
            tokenStorage.clear()
          }
        }
      }

      if (tokenStorage.get()) {
        try {
          const profile = await meApi.get()
          setUser(await settleAuthenticatedSession(profile))
          // Prod: background SSO refresh when stale token worked but ForwardAuth may be ready now.
          if (isAtlasPublicHost()) {
            void refreshAuthToken(2).then(async (fresh) => {
              if (!fresh) return
              try {
                const refreshed = await meApi.get()
                setUser(refreshed)
                await queryClient.invalidateQueries()
              } catch {
                /* keep current session */
              }
            })
          }
          return
        } catch {
          tokenStorage.clear()
        }
      }

      const attempts = isAtlasPublicHost() ? 4 : 2
      const ssoUser = await tryAuthentikSsoWithRetry(attempts)
      setUser(await settleAuthenticatedSession(ssoUser))
      setSsoFailed(!ssoUser)
    } finally {
      setLoading(false)
    }
  }, [])

  const retrySso = useCallback(async () => {
    setLoading(true)
    setSsoFailed(false)
    try {
      const token = await refreshAuthToken(isAtlasPublicHost() ? 4 : 2)
      if (token) {
        try {
          const profile = await meApi.get()
          setUser(await settleAuthenticatedSession(profile))
          return profile
        } catch {
          tokenStorage.clear()
        }
      }
      const ssoUser = await tryAuthentikSsoWithRetry(isAtlasPublicHost() ? 4 : 2)
      setUser(await settleAuthenticatedSession(ssoUser))
      setSsoFailed(!ssoUser)
      return ssoUser
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refreshUser()
  }, [refreshUser])

  const login = useCallback(async (username: string, password: string) => {
    const result = await authApi.login(username, password)
    tokenStorage.set(result.accessToken)
    const profile = await meApi.get()
    setUser(profile)
    setSsoFailed(false)
    await queryClient.invalidateQueries()
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
    queryClient.clear()
  }, [])

  const authReady =
    !loading && !!user && (isAtlasPublicHost() ? true : !!tokenStorage.get())

  const value = useMemo(
    () => ({ user, loading, authReady, ssoFailed, login, logout, refreshUser, retrySso }),
    [user, loading, authReady, ssoFailed, login, logout, refreshUser, retrySso],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}

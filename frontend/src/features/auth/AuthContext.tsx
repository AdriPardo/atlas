import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { meApi, authApi } from '../../shared/api/endpoints'
import { tokenStorage } from '../../shared/api/client'
import type { User } from '../../shared/types/api'
import { isAtlasPublicHost } from './authHost'

interface AuthContextValue {
  user: User | null
  loading: boolean
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
    const result = await authApi.sso()
    tokenStorage.set(result.accessToken)
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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [ssoFailed, setSsoFailed] = useState(false)

  const refreshUser = useCallback(async () => {
    setLoading(true)
    setSsoFailed(false)
    try {
      if (tokenStorage.get()) {
        try {
          setUser(await meApi.get())
          return
        } catch {
          tokenStorage.clear()
        }
      }

      // Behind Authentik ForwardAuth, Traefik injects X-authentik-* → mint Atlas JWT.
      const attempts = isAtlasPublicHost() ? 4 : 2
      const ssoUser = await tryAuthentikSsoWithRetry(attempts)
      setUser(ssoUser)
      setSsoFailed(!ssoUser)
    } finally {
      setLoading(false)
    }
  }, [])

  const retrySso = useCallback(async () => {
    setLoading(true)
    setSsoFailed(false)
    try {
      const ssoUser = await tryAuthentikSsoWithRetry(isAtlasPublicHost() ? 4 : 2)
      setUser(ssoUser)
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
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, loading, ssoFailed, login, logout, refreshUser, retrySso }),
    [user, loading, ssoFailed, login, logout, refreshUser, retrySso],
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

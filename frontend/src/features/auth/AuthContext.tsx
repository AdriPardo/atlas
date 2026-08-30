import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { queryClient } from '../../app/queryClient'
import { meApi, authApi } from '../../shared/api/endpoints'
import { tokenStorage } from '../../shared/api/client'
import type { User } from '../../shared/types/api'
import { isAtlasPublicHost } from './authHost'

interface AuthContextValue {
  user: User | null
  loading: boolean
  /** True only after JWT is stored and /me returned 200. */
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

async function establishSession(): Promise<User | null> {
  if (tokenStorage.get()) {
    try {
      return await meApi.get()
    } catch {
      tokenStorage.clear()
    }
  }

  try {
    const mint = await authApi.sso()
    tokenStorage.set(mint.accessToken)
    return await meApi.get()
  } catch {
    tokenStorage.clear()
    return null
  }
}

/** Retry SSO — covers brief backend restarts / 502 while Traefik already authenticated. */
async function establishSessionWithRetry(attempts = 3): Promise<User | null> {
  for (let i = 0; i < attempts; i += 1) {
    const user = await establishSession()
    if (user && tokenStorage.get()) return user
    if (i < attempts - 1) {
      await sleep(350 * (i + 1))
    }
  }
  return null
}

function sessionReady(user: User | null): boolean {
  return !!user && !!tokenStorage.get()
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [authReady, setAuthReady] = useState(false)
  const [ssoFailed, setSsoFailed] = useState(false)
  const bootstrapGeneration = useRef(0)

  const applySession = useCallback((profile: User | null) => {
    const ready = sessionReady(profile)
    setUser(ready ? profile : null)
    setAuthReady(ready)
    if (!ready) {
      tokenStorage.clear()
    }
  }, [])

  const refreshUser = useCallback(async () => {
    const generation = ++bootstrapGeneration.current
    setLoading(true)
    setAuthReady(false)
    setSsoFailed(false)

    try {
      const attempts = isAtlasPublicHost() ? 4 : 2
      const profile = await establishSessionWithRetry(attempts)
      if (generation !== bootstrapGeneration.current) return
      applySession(profile)
      setSsoFailed(!profile)
    } finally {
      if (generation === bootstrapGeneration.current) {
        setLoading(false)
      }
    }
  }, [applySession])

  const retrySso = useCallback(async () => {
    const generation = ++bootstrapGeneration.current
    setLoading(true)
    setAuthReady(false)
    setSsoFailed(false)

    try {
      const attempts = isAtlasPublicHost() ? 4 : 2
      const profile = await establishSessionWithRetry(attempts)
      if (generation !== bootstrapGeneration.current) return null
      applySession(profile)
      setSsoFailed(!profile)
      return profile
    } finally {
      if (generation === bootstrapGeneration.current) {
        setLoading(false)
      }
    }
  }, [applySession])

  useEffect(() => {
    void refreshUser()
  }, [refreshUser])

  const login = useCallback(async (username: string, password: string) => {
    const result = await authApi.login(username, password)
    tokenStorage.set(result.accessToken)
    const profile = await meApi.get()
    applySession(profile)
    setSsoFailed(false)
    await queryClient.invalidateQueries()
  }, [applySession])

  const logout = useCallback(() => {
    bootstrapGeneration.current += 1
    tokenStorage.clear()
    setUser(null)
    setAuthReady(false)
    queryClient.clear()
  }, [])

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

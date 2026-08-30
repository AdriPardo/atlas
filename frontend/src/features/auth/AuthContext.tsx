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
import { tokenStorage } from '../../shared/api/tokenStorage'
import { bootstrapSession, resetSessionCache } from '../../shared/api/sessionBootstrap'
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

function sessionReady(user: User | null): boolean {
  return !!user && !!tokenStorage.get()
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [authReady, setAuthReady] = useState(false)
  const [ssoFailed, setSsoFailed] = useState(false)
  const bootstrapStarted = useRef(false)

  const applySession = useCallback((profile: User | null) => {
    const ready = sessionReady(profile)
    setUser(ready ? profile : null)
    setAuthReady(ready)
    if (!ready) {
      tokenStorage.clear()
      resetSessionCache()
    }
  }, [])

  const runBootstrap = useCallback(async (force = false) => {
    if (force) {
      resetSessionCache()
    }
    const attempts = isAtlasPublicHost() ? 4 : 2
    const profile = await bootstrapSession(attempts)
    applySession(profile)
    setSsoFailed(!profile)
    return profile
  }, [applySession])

  const refreshUser = useCallback(async () => {
    setLoading(true)
    setSsoFailed(false)
    try {
      await runBootstrap(true)
    } finally {
      setLoading(false)
    }
  }, [runBootstrap])

  const retrySso = useCallback(async () => {
    setLoading(true)
    setSsoFailed(false)
    try {
      return await runBootstrap(true)
    } finally {
      setLoading(false)
    }
  }, [runBootstrap])

  // Single bootstrap on mount — StrictMode-safe (ref guard, never authReady=false mid-session).
  useEffect(() => {
    if (bootstrapStarted.current) return
    bootstrapStarted.current = true

    let cancelled = false
    ;(async () => {
      setLoading(true)
      setSsoFailed(false)
      try {
        const attempts = isAtlasPublicHost() ? 4 : 2
        const profile = await bootstrapSession(attempts)
        if (!cancelled) {
          applySession(profile)
          setSsoFailed(!profile)
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [applySession])

  const login = useCallback(async (username: string, password: string) => {
    resetSessionCache()
    const result = await authApi.login(username, password)
    tokenStorage.set(result.accessToken)
    const profile = await meApi.get()
    applySession(profile)
    setSsoFailed(false)
    await queryClient.invalidateQueries()
  }, [applySession])

  const logout = useCallback(() => {
    tokenStorage.clear()
    resetSessionCache()
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

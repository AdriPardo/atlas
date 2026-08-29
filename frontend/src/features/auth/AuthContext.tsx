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
import {
  getAuthBootstrapPhase,
  resetAuthBootstrap,
  resolveAuthBootstrap,
} from '../../shared/api/authBootstrap'
import {
  clearSsoRedirectFlag,
  redirectToSsoBootstrap,
} from '../../shared/api/authSession'
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
  retrySso: () => Promise<User | null>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

/**
 * Establish session on the Authentik edge: bootstrap mints JWT (full page), then /me via Bearer.
 * Never redirect again when /me fails — that caused outpost ↔ SPA loops.
 */
async function establishSession(): Promise<User | null> {
  if (isAtlasPublicHost()) {
    const token = tokenStorage.get()
    if (!token) {
      redirectToSsoBootstrap()
      return null
    }

    try {
      const profile = await meApi.get()
      clearSsoRedirectFlag()
      return profile
    } catch {
      tokenStorage.clear()
      clearSsoRedirectFlag()
      return null
    }
  }

  if (!tokenStorage.get()) return null

  try {
    return await meApi.get()
  } catch {
    tokenStorage.clear()
    return null
  }
}

function finishBootstrap(user: User | null) {
  const ok = !!user && !!tokenStorage.get()
  if (getAuthBootstrapPhase() === 'pending') {
    resolveAuthBootstrap(ok)
  }
  return ok
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [ssoFailed, setSsoFailed] = useState(false)

  const refreshUser = useCallback(async () => {
    setLoading(true)
    setSsoFailed(false)
    if (getAuthBootstrapPhase() !== 'pending') {
      resetAuthBootstrap()
    }
    try {
      const profile = await establishSession()
      setUser(profile)
      setSsoFailed(isAtlasPublicHost() && !profile)
      if (finishBootstrap(profile) && profile) {
        await queryClient.invalidateQueries()
      }
    } catch {
      setUser(null)
      setSsoFailed(isAtlasPublicHost())
      finishBootstrap(null)
    } finally {
      setLoading(false)
    }
  }, [])

  const retrySso = useCallback(async () => {
    setLoading(true)
    setSsoFailed(false)
    clearSsoRedirectFlag()
    resetAuthBootstrap()
    redirectToSsoBootstrap('/')
    return null
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
    clearSsoRedirectFlag()
    finishBootstrap(profile)
    await queryClient.invalidateQueries()
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    clearSsoRedirectFlag()
    setUser(null)
    queryClient.clear()
    if (getAuthBootstrapPhase() === 'pending') {
      resolveAuthBootstrap(false)
    }
    resetAuthBootstrap()
  }, [])

  const authReady = !loading && !!user && !!tokenStorage.get()

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

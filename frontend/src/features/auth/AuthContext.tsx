import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import axios from 'axios'
import { queryClient } from '../../app/queryClient'
import {
  getAuthBootstrapPhase,
  resetAuthBootstrap,
  resolveAuthBootstrap,
} from '../../shared/api/authBootstrap'
import {
  clearSsoRedirectFlag,
  consumeSsoError,
  isSsoRedirectInFlight,
  mapMeFailureStatus,
  redirectToSsoBootstrap,
  type SsoFailureCode,
} from '../../shared/api/authSession'
import { tokenStorage } from '../../shared/api/client'
import { meApi, authApi } from '../../shared/api/endpoints'
import type { User } from '../../shared/types/api'
import { isAtlasPublicHost } from './authHost'

interface AuthContextValue {
  user: User | null
  loading: boolean
  authReady: boolean
  ssoFailure: SsoFailureCode | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  refreshUser: () => Promise<void>
  retrySso: () => Promise<User | null>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

type EstablishResult = {
  user: User | null
  redirecting?: boolean
  failure?: SsoFailureCode
}

function syncTokenFromCookie(): void {
  if (tokenStorage.get()) return
  const fromCookie = document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith('atlas.token='))
  if (!fromCookie) return
  const value = decodeURIComponent(fromCookie.slice('atlas.token='.length))
  if (value) tokenStorage.set(value)
}

/**
 * Prod: try /me first (bootstrap may have set HttpOnly/session cookie). Only redirect to
 * bootstrap when unauthenticated. Never treat "no localStorage yet" as failure.
 */
async function establishSession(): Promise<EstablishResult> {
  if (isAtlasPublicHost()) {
    const hadStoredToken = !!tokenStorage.get()

    try {
      const profile = await meApi.get()
      syncTokenFromCookie()
      clearSsoRedirectFlag()
      return { user: profile }
    } catch (err) {
      const status = axios.isAxiosError(err) ? err.response?.status : undefined
      tokenStorage.clear()
      clearSsoRedirectFlag()

      if (hadStoredToken) {
        return { user: null, failure: consumeSsoError() ?? mapMeFailureStatus(status) }
      }

      if (redirectToSsoBootstrap()) {
        return { user: null, redirecting: true }
      }
      return { user: null, failure: consumeSsoError() ?? 'redirect_blocked' }
    }
  }

  if (!tokenStorage.get()) {
    return { user: null }
  }

  try {
    return { user: await meApi.get() }
  } catch {
    tokenStorage.clear()
    return { user: null }
  }
}

function finishBootstrap(user: User | null) {
  const ok = !!user
  if (getAuthBootstrapPhase() === 'pending') {
    resolveAuthBootstrap(ok)
  }
  return ok
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [ssoFailure, setSsoFailure] = useState<SsoFailureCode | null>(null)

  const refreshUser = useCallback(async () => {
    setLoading(true)
    setSsoFailure(null)
    if (getAuthBootstrapPhase() !== 'pending') {
      resetAuthBootstrap()
    }
    try {
      const result = await establishSession()
      if (result.redirecting) {
        setUser(null)
        setSsoFailure(null)
        setLoading(true)
        return
      }
      setUser(result.user)
      setSsoFailure(result.failure ?? null)
      if (finishBootstrap(result.user) && result.user) {
        await queryClient.invalidateQueries()
      }
    } catch {
      setUser(null)
      setSsoFailure(isAtlasPublicHost() ? 'mint_failed' : null)
      finishBootstrap(null)
    } finally {
      if (!isSsoRedirectInFlight()) {
        setLoading(false)
      }
    }
  }, [])

  const retrySso = useCallback(async () => {
    setLoading(true)
    setSsoFailure(null)
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
    setSsoFailure(null)
    clearSsoRedirectFlag()
    finishBootstrap(profile)
    await queryClient.invalidateQueries()
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    clearSsoRedirectFlag()
    setUser(null)
    setSsoFailure(null)
    queryClient.clear()
    if (getAuthBootstrapPhase() === 'pending') {
      resolveAuthBootstrap(false)
    }
    resetAuthBootstrap()
  }, [])

  const authReady = !loading && !!user

  const value = useMemo(
    () => ({ user, loading, authReady, ssoFailure, login, logout, refreshUser, retrySso }),
    [user, loading, authReady, ssoFailure, login, logout, refreshUser, retrySso],
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

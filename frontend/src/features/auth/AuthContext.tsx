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
  clearSsoAttempt,
  clearSsoRedirectFlag,
  consumeSsoError,
  hasExhaustedSsoAttempts,
  isSsoRedirectInFlight,
  isTerminalSsoFailure,
  redirectToSsoBootstrap,
  type SsoFailureCode,
} from '../../shared/api/authSession'
import { tokenStorage } from '../../shared/api/client'
import { meApi, authApi } from '../../shared/api/endpoints'
import type { User } from '../../shared/types/api'
import { isAtlasPublicHost, redirectToAuthentikSignOut } from './authHost'

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

/** Bootstrap redirect may pass JWT in hash — read once then strip from URL. */
function syncTokenFromHash(): void {
  if (tokenStorage.get()) return
  const hash = window.location.hash
  if (!hash.startsWith('#atlas.token=')) return
  const value = decodeURIComponent(hash.slice('#atlas.token='.length))
  if (!value) return
  tokenStorage.set(value)
  window.history.replaceState(null, '', window.location.pathname + window.location.search)
}

function syncTokenFromBootstrap(): void {
  syncTokenFromHash()
  syncTokenFromCookie()
}

/**
 * Prod SSO:
 * 1. Bootstrap cookie from prior redirect → sync to localStorage → /me
 * 2. No JWT → one full-page bootstrap (Authentik session already exists from Traefik)
 * 3. Bootstrap fail or /me fail after bootstrap → error ONCE, no auto-retry loop
 */
async function establishSession(): Promise<EstablishResult> {
  if (isAtlasPublicHost()) {
    const bootstrapError = consumeSsoError()
    if (bootstrapError && isTerminalSsoFailure(bootstrapError)) {
      return { user: null, failure: bootstrapError }
    }

    syncTokenFromBootstrap()

    if (tokenStorage.get()) {
      try {
        const profile = await meApi.get()
        clearSsoRedirectFlag()
        clearSsoAttempt()
        return { user: profile }
      } catch {
        tokenStorage.clear()
        clearSsoRedirectFlag()
        if (hasExhaustedSsoAttempts()) {
          return {
            user: null,
            failure: bootstrapError ?? 'token_rejected',
          }
        }
      }
    }

    if (hasExhaustedSsoAttempts()) {
      return {
        user: null,
        failure: bootstrapError ?? 'redirect_blocked',
      }
    }

    if (redirectToSsoBootstrap('/')) {
      return { user: null, redirecting: true }
    }

    return { user: null, failure: bootstrapError ?? 'redirect_blocked' }
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
      setSsoFailure(hasExhaustedSsoAttempts() ? 'mint_failed' : null)
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
    clearSsoAttempt()
    clearSsoRedirectFlag()
    tokenStorage.clear()
    if (redirectToSsoBootstrap('/')) {
      return null
    }
    setSsoFailure('redirect_blocked')
    setLoading(false)
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
    clearSsoAttempt()
    finishBootstrap(profile)
    await queryClient.invalidateQueries()
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    clearSsoRedirectFlag()
    clearSsoAttempt()
    setUser(null)
    setSsoFailure(null)
    queryClient.clear()
    if (getAuthBootstrapPhase() === 'pending') {
      resolveAuthBootstrap(false)
    }
    resetAuthBootstrap()
    if (isAtlasPublicHost()) {
      redirectToAuthentikSignOut()
    }
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

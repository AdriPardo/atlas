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
  consumeSsoError,
  isSsoRedirectInFlight,
  isTerminalSsoFailure,
  markBootstrapReturn,
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

function syncTokenFromCookie(): boolean {
  if (tokenStorage.get()) return true
  const fromCookie = document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith('atlas.token='))
  if (!fromCookie) return false
  const value = decodeURIComponent(fromCookie.slice('atlas.token='.length))
  if (!value) return false
  tokenStorage.set(value)
  return true
}

/** Bootstrap redirect passes JWT in hash — read once then strip from URL. */
function syncTokenFromHash(): boolean {
  if (tokenStorage.get()) return true
  const hash = window.location.hash
  if (!hash.startsWith('#atlas.token=')) return false
  const value = decodeURIComponent(hash.slice('#atlas.token='.length))
  if (!value) return false
  tokenStorage.set(value)
  window.history.replaceState(null, '', window.location.pathname + window.location.search)
  return true
}

function syncTokenFromBootstrap(): boolean {
  const fromBootstrap = syncTokenFromHash() || syncTokenFromCookie()
  if (fromBootstrap) {
    markBootstrapReturn()
  }
  return fromBootstrap
}

/**
 * Prod SSO:
 * 1. Returning from bootstrap → sync JWT from hash/cookie → /me
 * 2. No JWT yet → full-page bootstrap (Authentik session exists from Traefik)
 * 3. Bootstrap/server errors → show once; user retries manually
 */
async function establishSession(): Promise<EstablishResult> {
  if (isAtlasPublicHost()) {
    const bootstrapError = consumeSsoError()
    if (bootstrapError && isTerminalSsoFailure(bootstrapError)) {
      return { user: null, failure: bootstrapError }
    }

    const returnedFromBootstrap = syncTokenFromBootstrap()

    if (tokenStorage.get()) {
      try {
        const profile = await meApi.get()
        markBootstrapReturn()
        return { user: profile }
      } catch {
        tokenStorage.clear()
        clearSsoRedirectFlag()
        if (bootstrapError) {
          return { user: null, failure: bootstrapError }
        }
      }
    } else if (bootstrapError) {
      return { user: null, failure: bootstrapError }
    }

    if (returnedFromBootstrap) {
      return { user: null, failure: bootstrapError ?? 'token_rejected' }
    }

    if (redirectToSsoBootstrap('/')) {
      return { user: null, redirecting: true }
    }

    return { user: null, failure: 'redirect_blocked' }
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
      setSsoFailure('mint_failed')
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
    tokenStorage.clear()
    clearSsoRedirectFlag()
    sessionStorage.removeItem('atlas.sso.error')
    window.location.replace(
      `${window.location.origin}/auth/sso/bootstrap?returnTo=${encodeURIComponent('/')}`,
    )
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
    markBootstrapReturn()
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

import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { queryClient } from '../../app/queryClient'
import { authApi } from '../../shared/api/endpoints'
import {
  activateSessionToken,
  bootstrapAuthSession,
  getSessionUser,
  isSessionReady,
  resetAuthSession,
} from '../../shared/api/authSession'
import type { User } from '../../shared/types/api'

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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => getSessionUser())
  const [authReady, setAuthReady] = useState(() => isSessionReady())
  const [loading, setLoading] = useState(false)
  const [ssoFailed, setSsoFailed] = useState(() => !isSessionReady())

  const applyFromSession = useCallback((profile: User | null) => {
    setUser(profile)
    setAuthReady(isSessionReady())
    setSsoFailed(!profile)
  }, [])

  const refreshUser = useCallback(async () => {
    setLoading(true)
    try {
      resetAuthSession()
      const profile = await bootstrapAuthSession()
      applyFromSession(profile)
    } finally {
      setLoading(false)
    }
  }, [applyFromSession])

  const retrySso = useCallback(async () => {
    setLoading(true)
    try {
      resetAuthSession()
      const profile = await bootstrapAuthSession()
      applyFromSession(profile)
      return profile
    } finally {
      setLoading(false)
    }
  }, [applyFromSession])

  const login = useCallback(
    async (username: string, password: string) => {
      const result = await authApi.login(username, password)
      const profile = await activateSessionToken(result.accessToken)
      applyFromSession(profile)
      await queryClient.invalidateQueries()
    },
    [applyFromSession],
  )

  const logout = useCallback(() => {
    resetAuthSession()
    setUser(null)
    setAuthReady(false)
    setSsoFailed(true)
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

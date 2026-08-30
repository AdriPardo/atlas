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
import { redirectToSsoMint } from '../../shared/api/authSession'
import { tokenStorage } from '../../shared/api/client'
import { meApi, authApi } from '../../shared/api/endpoints'
import type { User } from '../../shared/types/api'
import { isAtlasPublicHost, redirectToAuthentikSignOut } from './authHost'

interface AuthContextValue {
  user: User | null
  loading: boolean
  authReady: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  refreshUser: () => Promise<void>
  enterWithAuthentik: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

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

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const refreshUser = useCallback(async () => {
    setLoading(true)

    if (isAtlasPublicHost()) {
      syncTokenFromCookie()

      if (tokenStorage.get()) {
        try {
          const profile = await meApi.get()
          setUser(profile)
          setLoading(false)
          return
        } catch {
          tokenStorage.clear()
        }
      }

      redirectToSsoMint()
      return
    }

    if (!tokenStorage.get()) {
      setUser(null)
      setLoading(false)
      return
    }

    try {
      setUser(await meApi.get())
    } catch {
      tokenStorage.clear()
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  const enterWithAuthentik = useCallback(() => {
    tokenStorage.clear()
    redirectToSsoMint()
  }, [])

  useEffect(() => {
    void refreshUser()
  }, [refreshUser])

  const login = useCallback(async (username: string, password: string) => {
    const result = await authApi.login(username, password)
    tokenStorage.set(result.accessToken)
    setUser(await meApi.get())
    await queryClient.invalidateQueries()
  }, [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    setUser(null)
    queryClient.clear()
    if (isAtlasPublicHost()) {
      redirectToAuthentikSignOut()
    }
  }, [])

  const authReady = !loading && !!user

  const value = useMemo(
    () => ({ user, loading, authReady, login, logout, refreshUser, enterWithAuthentik }),
    [user, loading, authReady, login, logout, refreshUser, enterWithAuthentik],
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

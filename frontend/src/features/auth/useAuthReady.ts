import { useAuth } from './AuthContext'

/** True when SSO session is ready for API queries. */
export function useAuthReady(): boolean {
  const { authReady } = useAuth()
  return authReady
}

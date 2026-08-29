import { useAuth } from './AuthContext'

/** True when bootstrap finished and session is ready for API queries. */
export function useAuthReady(): boolean {
  const { authReady } = useAuth()
  return authReady
}

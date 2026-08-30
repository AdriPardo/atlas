import { useAuth } from './AuthContext'

/** True when bootstrap finished and Atlas JWT session is ready for API queries. */
export function useAuthReady(): boolean {
  const { user, loading } = useAuth()
  return !loading && !!user
}

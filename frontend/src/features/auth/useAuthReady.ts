import { useAuth } from './AuthContext'
import { hasAuthToken } from '../../shared/api/authSession'

/** True when bootstrap finished and a bearer token is present for API calls. */
export function useAuthReady(): boolean {
  const { user, loading } = useAuth()
  return !loading && !!user && hasAuthToken()
}

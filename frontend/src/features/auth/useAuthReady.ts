import { isSessionReady } from '../../shared/api/authSession'
import { tokenStorage } from '../../shared/api/tokenStorage'
import { useAuth } from './AuthContext'

/** True when JWT is stored, /me succeeded, and bootstrap finished. */
export function useAuthReady(): boolean {
  const { authReady } = useAuth()
  return authReady && isSessionReady() && !!tokenStorage.get()
}

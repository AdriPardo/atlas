import { tokenStorage } from '../../shared/api/client'
import { useAuth } from './AuthContext'

/** True when JWT is stored, /me succeeded, and bootstrap finished. */
export function useAuthReady(): boolean {
  const { authReady } = useAuth()
  return authReady && !!tokenStorage.get()
}

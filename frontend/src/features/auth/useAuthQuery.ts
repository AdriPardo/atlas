import {
  useQuery,
  type QueryKey,
  type UseQueryOptions,
  type UseQueryResult,
} from '@tanstack/react-query'
import { useAuth } from './AuthContext'

/**
 * React Query wrapper — data fetches wait until auth bootstrap finished and user exists.
 */
export function useAuthQuery<
  TQueryFnData = unknown,
  TError = Error,
  TData = TQueryFnData,
  TQueryKey extends QueryKey = QueryKey,
>(
  options: UseQueryOptions<TQueryFnData, TError, TData, TQueryKey>,
): UseQueryResult<TData, TError> {
  const { loading, user } = useAuth()
  const userEnabled = options.enabled ?? true
  return useQuery({
    ...options,
    enabled: !loading && user != null && userEnabled,
  })
}

import {
  useQuery,
  type QueryKey,
  type UseQueryOptions,
  type UseQueryResult,
} from '@tanstack/react-query'
import { useAuthReady } from './useAuthReady'

/**
 * React Query wrapper — all data fetches wait for authReady (JWT stored + /me OK).
 * Pass `enabled: false` or extra conditions via options.enabled as usual.
 */
export function useAuthQuery<
  TQueryFnData = unknown,
  TError = Error,
  TData = TQueryFnData,
  TQueryKey extends QueryKey = QueryKey,
>(
  options: UseQueryOptions<TQueryFnData, TError, TData, TQueryKey>,
): UseQueryResult<TData, TError> {
  const authReady = useAuthReady()
  const userEnabled = options.enabled ?? true
  return useQuery({
    ...options,
    enabled: authReady && userEnabled,
  })
}

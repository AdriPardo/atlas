import { QueryClient } from '@tanstack/react-query'
import { isTransientApiError } from '../shared/api/queryErrors'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Edge/proxy 502s and brief backend restarts — same class of flake SSO already retries.
      retry: (failureCount, error) => failureCount < 3 && isTransientApiError(error),
      retryDelay: (attempt) => Math.min(500 * 2 ** attempt, 4000),
      refetchOnWindowFocus: false,
      // Dedupe remount refetches (StrictMode, layout + page) within 15s.
      staleTime: 15_000,
    },
  },
})

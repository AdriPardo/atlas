import { QueryClient } from '@tanstack/react-query'
import {
  isForbiddenApiError,
  isTransientApiError,
  isUnauthorizedApiError,
} from '../shared/api/queryErrors'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        // Never retry auth failures — avoids 403 storms and duplicate requests.
        if (isForbiddenApiError(error) || isUnauthorizedApiError(error)) {
          return false
        }
        return failureCount < 2 && isTransientApiError(error)
      },
      retryDelay: (attempt) => Math.min(500 * 2 ** attempt, 4000),
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
      staleTime: 30_000,
    },
  },
})

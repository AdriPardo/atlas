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
        if (isForbiddenApiError(error) || isUnauthorizedApiError(error)) {
          return false
        }
        return failureCount < 3 && isTransientApiError(error)
      },
      retryDelay: (attempt) => Math.min(500 * 2 ** attempt, 4000),
      refetchOnWindowFocus: false,
      staleTime: 15_000,
    },
  },
})

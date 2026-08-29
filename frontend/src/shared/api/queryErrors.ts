import axios from 'axios'

/** Transient failures worth automatic retry (edge/proxy blips, brief backend restarts). */
export function isTransientApiError(error: unknown): boolean {
  if (!axios.isAxiosError(error)) {
    return error instanceof TypeError || error instanceof DOMException
  }
  if (!error.response) {
    return true
  }
  const status = error.response.status
  return status === 408 || status === 429 || status === 500 || status === 502 || status === 503 || status === 504
}

export function getApiErrorStatus(error: unknown): number | undefined {
  return axios.isAxiosError(error) ? error.response?.status : undefined
}

export function isForbiddenApiError(error: unknown): boolean {
  return getApiErrorStatus(error) === 403
}

export function isUnauthorizedApiError(error: unknown): boolean {
  return getApiErrorStatus(error) === 401
}

export function getApiErrorMessage(error: unknown, fallback?: string): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status
    const body = error.response?.data as { message?: string; error?: string } | string | undefined
    const detail =
      typeof body === 'string'
        ? body
        : body && typeof body === 'object'
          ? body.message || body.error
          : undefined
    if (status === 403) {
      return (
        detail ??
        'You do not have permission for this resource. Ask a project admin for access or sign in again.'
      )
    }
    if (status === 401) {
      return detail ?? 'Session expired or invalid. Sign in again.'
    }
    if (status && detail) return `${detail} (${status})`
    if (status) return `${fallback ?? 'Request failed'} (${status}).`
    if (error.code === 'ECONNABORTED') return 'Request timed out. Try again.'
    if (!error.response) return 'Network error. Check your connection and try again.'
  }
  if (error instanceof Error && error.message) return error.message
  return fallback ?? 'Something went wrong. Check your connection and try again.'
}

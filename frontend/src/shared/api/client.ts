import axios, { type InternalAxiosRequestConfig } from 'axios'

const TOKEN_KEY = 'atlas.token'
export const SKIP_AUTH_RETRY_HEADER = 'X-Atlas-Skip-Auth-Retry'

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

type RetriableConfig = InternalAxiosRequestConfig & { _authRetry?: boolean }

export const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status
    const config = error.config as RetriableConfig | undefined
    const url = String(config?.url ?? '')

    if (url.includes('/auth/sso') || url.includes('/auth/login')) {
      return Promise.reject(error)
    }

    if (config?.headers?.[SKIP_AUTH_RETRY_HEADER]) {
      return Promise.reject(error)
    }

    if ((status === 401 || status === 403) && config && !config._authRetry) {
      config._authRetry = true
      const { refreshAtlasSession } = await import('./sessionRefresh')
      const refreshed = await refreshAtlasSession()
      if (refreshed) {
        const token = tokenStorage.get()
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return api(config)
      }
    }

    if (status === 401) {
      tokenStorage.clear()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }

    return Promise.reject(error)
  },
)

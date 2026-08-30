import axios, { type InternalAxiosRequestConfig } from 'axios'
import { tokenStorage } from './tokenStorage'

export const SKIP_AUTH_RETRY_HEADER = 'X-Atlas-Skip-Auth-Retry'

export { tokenStorage } from './tokenStorage'

type RetriableConfig = InternalAxiosRequestConfig & { _authRetry?: boolean }

export const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  } else {
    delete config.headers.Authorization
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

    if (config && !config._authRetry) {
      const token = tokenStorage.get()

      // 403 with token: one immediate retry (ForwardAuth/JWT timing blip) — no /me bootstrap.
      if (status === 403 && token) {
        config._authRetry = true
        config.headers.Authorization = `Bearer ${token}`
        return api(config)
      }

      // 401 or 403 without token: mint/verify session once, then retry original request.
      if (status === 401 || (status === 403 && !token)) {
        config._authRetry = true
        const { refreshAtlasSession } = await import('./sessionBootstrap')
        const refreshed = await refreshAtlasSession()
        if (refreshed) {
          const next = tokenStorage.get()
          if (next) {
            config.headers.Authorization = `Bearer ${next}`
          }
          return api(config)
        }
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

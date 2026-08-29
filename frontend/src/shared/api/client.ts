import axios, { type InternalAxiosRequestConfig } from 'axios'
import { isAtlasPublicHost } from '../../features/auth/authHost'
import { refreshAuthToken } from './authSession'
import { tokenStorage } from './tokenStorage'

export { tokenStorage } from './tokenStorage'

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _authRetry?: boolean
}

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
    const config = error.config as RetryableRequestConfig | undefined
    const status = error.response?.status
    const url = String(config?.url ?? '')

    if (config && !config._authRetry && (status === 401 || status === 403)) {
      if (url.includes('/auth/sso') || url.includes('/auth/login')) {
        return Promise.reject(error)
      }
      // Auth bootstrap owns /me failures — refreshing SSO here causes /sso + /me retry storms.
      if (status === 403 && url.endsWith('/me')) {
        return Promise.reject(error)
      }

      const newToken = await refreshAuthToken(status === 403 ? 2 : 3)
      if (newToken) {
        config._authRetry = true
        config.headers.Authorization = `Bearer ${newToken}`
        return api.request(config)
      }
    }

    if (status === 401) {
      if (url.includes('/auth/sso')) {
        return Promise.reject(error)
      }
      tokenStorage.clear()
      // Public Authentik edge: reload so ForwardAuth can re-establish session.
      if (isAtlasPublicHost()) {
        if (!window.location.pathname.startsWith('/login')) {
          window.location.reload()
        }
      } else if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }

    return Promise.reject(error)
  },
)

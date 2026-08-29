import axios, { type InternalAxiosRequestConfig } from 'axios'
import { isAtlasPublicHost } from '../../features/auth/authHost'
import {
  getAuthBootstrapPhase,
  isAuthBootstrapReady,
  waitForAuthBootstrap,
} from './authBootstrap'
import { isPublicAuthPath, redirectToSsoBootstrap, refreshAuthToken } from './authSession'
import { tokenStorage } from './tokenStorage'

export { tokenStorage } from './tokenStorage'

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _authRetry?: boolean
}

export const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use(async (config) => {
  const url = String(config.url ?? '')

  if (!isPublicAuthPath(url) && isAtlasPublicHost() && !isAuthBootstrapReady()) {
    const token = tokenStorage.get()
    // Bootstrap calls /me right after /sso mints JWT — must not wait on self.
    const bootstrapMe = token && url.endsWith('/me')
    if (!bootstrapMe) {
      const ready = await waitForAuthBootstrap()
      if (!ready) {
        return Promise.reject(new axios.CanceledError('Auth bootstrap incomplete'))
      }
    }
  }

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

    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }

    if (config && !config._authRetry && status === 401) {
      if (isPublicAuthPath(url)) {
        return Promise.reject(error)
      }
      // Bootstrap owns initial /me — interceptor refresh here caused /sso + /me storms.
      if (getAuthBootstrapPhase() === 'pending') {
        return Promise.reject(error)
      }

      const newToken = await refreshAuthToken()
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
      if (isAtlasPublicHost()) {
        redirectToSsoBootstrap()
      } else if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }

    return Promise.reject(error)
  },
)

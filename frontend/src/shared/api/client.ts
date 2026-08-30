import axios from 'axios'
import { tokenStorage } from './tokenStorage'

export const BOOTSTRAP_HEADER = 'X-Atlas-Bootstrap'
const PUBLIC_PATHS = ['/auth/login', '/auth/sso']

export { tokenStorage } from './tokenStorage'

function isPublicUrl(url: string): boolean {
  return PUBLIC_PATHS.some((path) => url.includes(path))
}

export const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  const url = String(config.url ?? '')
  const isBootstrap = Boolean(config.headers?.[BOOTSTRAP_HEADER])
  const token = tokenStorage.get()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  } else if (!isPublicUrl(url) && !isBootstrap) {
    return Promise.reject(new axios.CanceledError('Blocked: no auth token'))
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const url = String(error.config?.url ?? '')

    if (status === 401 && !isPublicUrl(url)) {
      tokenStorage.clear()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }

    return Promise.reject(error)
  },
)

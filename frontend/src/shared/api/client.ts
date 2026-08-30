import axios from 'axios'
import { isAtlasPublicHost } from '../../features/auth/authHost'
import { tokenStorage } from './tokenStorage'

export { tokenStorage } from './tokenStorage'

export const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
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
  (error) => {
    const status = error.response?.status
    const url = String(error.config?.url ?? '')

    if (status === 401 && !url.includes('/auth/sso') && !url.includes('/auth/login')) {
      tokenStorage.clear()
      if (isAtlasPublicHost()) {
        window.location.assign('/')
      } else if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }

    return Promise.reject(error)
  },
)

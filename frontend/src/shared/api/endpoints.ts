import { api } from './client'
import type {
  Application,
  DashboardStats,
  Deployment,
  Host,
  LoginResponse,
  PageResponse,
  User,
} from '../types/api'

export const authApi = {
  login: (username: string, password: string) =>
    api.post<LoginResponse>('/auth/login', { username, password }).then((r) => r.data),
}

export const meApi = {
  get: () => api.get<User>('/me').then((r) => r.data),
  stats: () => api.get<DashboardStats>('/dashboard/stats').then((r) => r.data),
}

export const applicationsApi = {
  list: (params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<Application>>('/applications', { params }).then((r) => r.data),
  get: (id: string) => api.get<Application>(`/applications/${id}`).then((r) => r.data),
  create: (body: Partial<Application>) =>
    api.post<Application>('/applications', body).then((r) => r.data),
  update: (id: string, body: Partial<Application>) =>
    api.put<Application>(`/applications/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/applications/${id}`),
}

export const hostsApi = {
  list: (params?: Record<string, string | number | boolean | undefined>) =>
    api.get<PageResponse<Host>>('/hosts', { params }).then((r) => r.data),
  get: (id: string) => api.get<Host>(`/hosts/${id}`).then((r) => r.data),
  create: (body: Partial<Host>) => api.post<Host>('/hosts', body).then((r) => r.data),
  update: (id: string, body: Partial<Host>) =>
    api.put<Host>(`/hosts/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/hosts/${id}`),
}

export const deploymentsApi = {
  list: (params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<Deployment>>('/deployments', { params }).then((r) => r.data),
  get: (id: string) => api.get<Deployment>(`/deployments/${id}`).then((r) => r.data),
  create: (body: { applicationId: string; hostId: string }) =>
    api.post<Deployment>('/deployments', body).then((r) => r.data),
  update: (id: string, body: Partial<Deployment>) =>
    api.put<Deployment>(`/deployments/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/deployments/${id}`),
}

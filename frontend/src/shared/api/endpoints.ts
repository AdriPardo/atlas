import { api } from './client'
import type {
  ContainerLogs,
  ContainerSnapshot,
  DashboardStats,
  DeployResponse,
  Deployment,
  Host,
  Job,
  LoginResponse,
  ObservabilitySettings,
  PageResponse,
  Project,
  SecretMeta,
  Service,
  User,
} from '../types/api'

export const authApi = {
  login: (username: string, password: string) =>
    api.post<LoginResponse>('/auth/login', { username, password }).then((r) => r.data),
  /** Mint Atlas JWT from Authentik ForwardAuth headers (no body). */
  sso: () => api.get<LoginResponse>('/auth/sso').then((r) => r.data),
}

export const meApi = {
  get: () => api.get<User>('/me').then((r) => r.data),
  stats: () => api.get<DashboardStats>('/dashboard/stats').then((r) => r.data),
}

export const projectsApi = {
  list: (params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<Project>>('/projects', { params }).then((r) => r.data),
  get: (id: string) => api.get<Project>(`/projects/${id}`).then((r) => r.data),
  create: (body: {
    name: string
    description?: string
    repositoryUrl: string
    branch: string
    composePath: string
    domain?: string
  }) => api.post<Project>('/projects', body).then((r) => r.data),
  update: (id: string, body: { name: string; description?: string; status: string }) =>
    api.put<Project>(`/projects/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/projects/${id}`),
  listServices: (projectId: string, params?: Record<string, string | number | undefined>) =>
    api
      .get<PageResponse<Service>>(`/projects/${projectId}/services`, { params })
      .then((r) => r.data),
  createService: (
    projectId: string,
    body: {
      name?: string
      repositoryUrl: string
      branch: string
      composePath: string
      domain?: string
      environment?: string
    },
  ) => api.post<Service>(`/projects/${projectId}/services`, body).then((r) => r.data),
  deploy: (projectId: string, hostId: string) =>
    api.post<DeployResponse>(`/projects/${projectId}/deploy`, { hostId }).then((r) => r.data),
}

export const servicesApi = {
  list: (params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<Service>>('/services', { params }).then((r) => r.data),
  get: (id: string) => api.get<Service>(`/services/${id}`).then((r) => r.data),
  update: (
    id: string,
    body: {
      name: string
      repositoryUrl: string
      branch: string
      composePath: string
      domain?: string
      environment?: string
      status: string
    },
  ) => api.put<Service>(`/services/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/services/${id}`),
  deploy: (id: string, hostId: string) =>
    api.post<DeployResponse>(`/services/${id}/deploy`, { hostId }).then((r) => r.data),
}

export const hostsApi = {
  list: (params?: Record<string, string | number | boolean | undefined>) =>
    api.get<PageResponse<Host>>('/hosts', { params }).then((r) => r.data),
  get: (id: string) => api.get<Host>(`/hosts/${id}`).then((r) => r.data),
  create: (body: Partial<Host>) => api.post<Host>('/hosts', body).then((r) => r.data),
  update: (id: string, body: Partial<Host>) =>
    api.put<Host>(`/hosts/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/hosts/${id}`),
  sync: (id: string) => api.post<Job>(`/hosts/${id}/sync`).then((r) => r.data),
  containers: (id: string) =>
    api.get<ContainerSnapshot[]>(`/hosts/${id}/containers`).then((r) => r.data),
  containerLogs: (id: string, containerRef: string, tail?: number) =>
    api
      .get<ContainerLogs>(`/hosts/${id}/containers/${encodeURIComponent(containerRef)}/logs`, {
        params: tail ? { tail } : undefined,
      })
      .then((r) => r.data),
  restartContainer: (id: string, containerRef: string) =>
    api.post(`/hosts/${id}/containers/${encodeURIComponent(containerRef)}/restart`),
}

export const settingsApi = {
  observability: () =>
    api.get<ObservabilitySettings>('/settings/observability').then((r) => r.data),
}

export const deploymentsApi = {
  list: (params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<Deployment>>('/deployments', { params }).then((r) => r.data),
  get: (id: string) => api.get<Deployment>(`/deployments/${id}`).then((r) => r.data),
  create: (body: { serviceId: string; hostId: string }) =>
    api.post<Deployment>('/deployments', body).then((r) => r.data),
  update: (id: string, body: Partial<Deployment>) =>
    api.put<Deployment>(`/deployments/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/deployments/${id}`),
}

export const jobsApi = {
  list: (params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<Job>>('/jobs', { params }).then((r) => r.data),
  get: (id: string) => api.get<Job>(`/jobs/${id}`).then((r) => r.data),
}

export const secretsApi = {
  list: () => api.get<SecretMeta[]>('/secrets').then((r) => r.data),
  create: (body: { name: string; value: string }) =>
    api.post<SecretMeta>('/secrets', body).then((r) => r.data),
}

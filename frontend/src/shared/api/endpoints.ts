import { api } from './client'
import type {
  AlertEventType,
  AlertRule,
  AuditEntry,
  ContainerLogs,
  ContainerSnapshot,
  CronJob,
  CronTargetType,
  DashboardStats,
  DeployResponse,
  Deployment,
  DomainRecord,
  Host,
  Job,
  LoginResponse,
  NotificationChannel,
  NotificationChannelType,
  ObservabilitySettings,
  AutoDeployResult,
  PageResponse,
  Pipeline,
  PipelineRun,
  PlanEntitlements,
  Project,
  ProjectMembership,
  ProjectSecretEntry,
  ProjectDatabaseStatus,
  ProjectDatabaseProvisionResult,
  ProjectDatabaseCredential,
  ProjectDatabaseCredentialListItem,
  SecretMeta,
  Service,
  ServiceExposure,
  PlacementMode,
  TraefikMetadata,
  TunnelIngress,
  DnsCname,
  UsageRecord,
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
    composePath?: string
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
      composePath?: string
      domain?: string
      environment?: string
    },
  ) => api.post<Service>(`/projects/${projectId}/services`, body).then((r) => r.data),
  deploy: (projectId: string, body?: { hostId?: string; exposure?: ServiceExposure; placementMode?: PlacementMode }) =>
    api.post<DeployResponse>(`/projects/${projectId}/deploy`, body ?? {}).then((r) => r.data),
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
      composePath?: string
      domain?: string
      environment?: string
      status: string
    },
  ) => api.put<Service>(`/services/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/services/${id}`),
  deploy: (id: string, body?: { hostId?: string; exposure?: ServiceExposure; placementMode?: PlacementMode }) =>
    api.post<DeployResponse>(`/services/${id}/deploy`, body ?? {}).then((r) => r.data),
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

export const pipelinesApi = {
  list: (params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<Pipeline>>('/pipelines', { params }).then((r) => r.data),
  get: (id: string) => api.get<Pipeline>(`/pipelines/${id}`).then((r) => r.data),
  create: (body: {
    projectId: string
    name: string
    serviceId: string
    hostId?: string
  }) => api.post<Pipeline>('/pipelines', body).then((r) => r.data),
  enableAutoDeploy: (body: { serviceId: string; hostId?: string; publicBaseUrl?: string }) =>
    api.post<AutoDeployResult>('/pipelines/enable-auto-deploy', body).then((r) => r.data),
  update: (id: string, body: { name: string; serviceId: string; hostId?: string | null }) =>
    api.put<Pipeline>(`/pipelines/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/pipelines/${id}`),
  run: (id: string) => api.post<PipelineRun>(`/pipelines/${id}/runs`).then((r) => r.data),
  rotateWebhookToken: (id: string) =>
    api.post<Pipeline>(`/pipelines/${id}/webhook-token/rotate`).then((r) => r.data),
  listRuns: (id: string, params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<PipelineRun>>(`/pipelines/${id}/runs`, { params }).then((r) => r.data),
}

export const membershipsApi = {
  list: (projectId: string) =>
    api.get<ProjectMembership[]>(`/projects/${projectId}/memberships`).then((r) => r.data),
  add: (projectId: string, body: { userId: string; role: string }) =>
    api.post<ProjectMembership>(`/projects/${projectId}/memberships`, body).then((r) => r.data),
  update: (projectId: string, membershipId: string, body: { role: string }) =>
    api
      .put<ProjectMembership>(`/projects/${projectId}/memberships/${membershipId}`, body)
      .then((r) => r.data),
  remove: (projectId: string, membershipId: string) =>
    api.delete(`/projects/${projectId}/memberships/${membershipId}`),
}

export const domainsApi = {
  list: (projectId: string) =>
    api.get<DomainRecord[]>(`/projects/${projectId}/domains`).then((r) => r.data),
  create: (projectId: string, body: { hostname: string; serviceId?: string }) =>
    api.post<DomainRecord>(`/projects/${projectId}/domains`, body).then((r) => r.data),
  verify: (domainId: string) =>
    api.post<DomainRecord>(`/domains/${domainId}/verify`).then((r) => r.data),
  remove: (domainId: string) => api.delete(`/domains/${domainId}`),
  traefik: (domainId: string) =>
    api.get<TraefikMetadata>(`/domains/${domainId}/traefik`).then((r) => r.data),
  tunnelIngress: (domainId: string) =>
    api.get<TunnelIngress>(`/domains/${domainId}/tunnel-ingress`).then((r) => r.data),
  ensureTunnel: (domainId: string) =>
    api.post<TunnelIngress>(`/domains/${domainId}/tunnel-ingress/ensure`).then((r) => r.data),
  dnsCname: (domainId: string) =>
    api.get<DnsCname>(`/domains/${domainId}/dns-cname`).then((r) => r.data),
  ensureDnsCname: (domainId: string) =>
    api.post<DnsCname>(`/domains/${domainId}/dns-cname/ensure`).then((r) => r.data),
}

export const auditApi = {
  list: (params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<AuditEntry>>('/audit', { params }).then((r) => r.data),
}

export const billingApi = {
  usage: (params?: Record<string, string | number | undefined>) =>
    api.get<PageResponse<UsageRecord>>('/billing/usage', { params }).then((r) => r.data),
  entitlements: () => api.get<PlanEntitlements>('/billing/entitlements').then((r) => r.data),
}

export const notificationChannelsApi = {
  list: () => api.get<NotificationChannel[]>('/notification-channels').then((r) => r.data),
  create: (body: { name: string; type: NotificationChannelType; target: string }) =>
    api.post<NotificationChannel>('/notification-channels', body).then((r) => r.data),
  remove: (channelId: string) => api.delete(`/notification-channels/${channelId}`),
}

export const alertsApi = {
  list: () => api.get<AlertRule[]>('/alerts').then((r) => r.data),
  create: (body: {
    name: string
    eventType: AlertEventType
    projectId?: string
    channelId: string
  }) => api.post<AlertRule>('/alerts', body).then((r) => r.data),
  silence: (ruleId: string) => api.post<AlertRule>(`/alerts/${ruleId}/silence`).then((r) => r.data),
  remove: (ruleId: string) => api.delete(`/alerts/${ruleId}`),
}

export const cronJobsApi = {
  list: () => api.get<CronJob[]>('/cron-jobs').then((r) => r.data),
  create: (body: {
    name: string
    cronExpression: string
    targetType: CronTargetType
    targetId?: string
  }) => api.post<CronJob>('/cron-jobs', body).then((r) => r.data),
  update: (
    id: string,
    body: {
      name: string
      cronExpression: string
      targetType: CronTargetType
      targetId?: string | null
      enabled: boolean
    },
  ) => api.put<CronJob>(`/cron-jobs/${id}`, body).then((r) => r.data),
  remove: (id: string) => api.delete(`/cron-jobs/${id}`),
}

export const usersApi = {
  list: () => api.get<User[]>('/users').then((r) => r.data),
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

export const projectSecretsApi = {
  list: (projectId: string) =>
    api.get<ProjectSecretEntry[]>(`/projects/${projectId}/secrets`).then((r) => r.data),
  create: (projectId: string, body: { name: string; value: string }) =>
    api.post<SecretMeta>(`/projects/${projectId}/secrets`, body).then((r) => r.data),
  link: (projectId: string, body: { secretId: string; alias?: string }) =>
    api
      .post<ProjectSecretEntry>(`/projects/${projectId}/secrets/bindings`, body)
      .then((r) => r.data),
  unlink: (projectId: string, bindingId: string) =>
    api.delete(`/projects/${projectId}/secrets/bindings/${bindingId}`),
  removeOwned: (projectId: string, secretId: string) =>
    api.delete(`/projects/${projectId}/secrets/${secretId}`),
}

export const projectDatabaseApi = {
  status: (projectId: string) =>
    api.get<ProjectDatabaseStatus>(`/projects/${projectId}/database`).then((r) => r.data),
  provision: (projectId: string) =>
    api
      .post<ProjectDatabaseProvisionResult>(`/projects/${projectId}/database/provision`)
      .then((r) => r.data),
  listCredentials: (projectId: string) =>
    api
      .get<ProjectDatabaseCredentialListItem[]>(`/projects/${projectId}/database/credentials`)
      .then((r) => r.data),
  issueCredential: (
    projectId: string,
    body?: { profile?: string; ttlMinutes?: number },
  ) =>
    api
      .post<ProjectDatabaseCredential>(`/projects/${projectId}/database/credentials`, body ?? {})
      .then((r) => r.data),
  revokeCredential: (projectId: string, role: string) =>
    api.delete(`/projects/${projectId}/database/credentials/${encodeURIComponent(role)}`),
}

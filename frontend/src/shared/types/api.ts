export type Role = 'ADMIN' | 'OPERATOR'

export type ProjectStatus =
  | 'REGISTERED'
  | 'READY'
  | 'DEPLOYING'
  | 'RUNNING'
  | 'STOPPED'
  | 'FAILED'

/** @deprecated Use ProjectStatus */
export type ApplicationStatus = ProjectStatus

export type ServiceStatus = ProjectStatus

export type DeploymentStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED'

export type ConnectionType = 'LOCAL' | 'SSH'

export type JobStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED'

export type JobType = 'DEPLOY_SERVICE' | 'SYNC_HOST'

export type PipelineRunStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  sort: string
}

export interface User {
  id: string
  username: string
  role: Role
}

export interface Project {
  id: string
  organizationId: string
  name: string
  slug: string
  description: string
  status: ProjectStatus
  createdAt: string
  updatedAt: string
}

export interface Service {
  id: string
  projectId: string
  name: string
  repositoryUrl: string
  branch: string
  composePath: string
  domain: string
  environment: string
  status: ServiceStatus
  createdAt: string
  updatedAt: string
}

/** @deprecated Prefer Project + Service */
export interface Application {
  id: string
  name: string
  description: string
  repositoryUrl: string
  branch: string
  composePath: string
  domain: string
  status: ApplicationStatus
  createdAt: string
  updatedAt: string
}

export interface Host {
  id: string
  hostname: string
  ip: string
  operatingSystem: string
  dockerVersion: string
  online: boolean
  connectionType: ConnectionType
  sshUser: string | null
  sshPort: number
  sshPrivateKeySecretId: string | null
  createdAt: string
  updatedAt: string
}

export interface Deployment {
  id: string
  serviceId: string
  /** @deprecated alias of serviceId */
  applicationId?: string
  hostId: string
  status: DeploymentStatus
  startedAt: string | null
  finishedAt: string | null
  logs: string
  createdAt: string
  updatedAt: string
}

export interface Job {
  id: string
  type: JobType
  payload: string
  status: JobStatus
  attempts: number
  maxAttempts: number
  availableAt: string
  lockedAt: string | null
  lockedBy: string | null
  startedAt: string | null
  finishedAt: string | null
  lastError: string | null
  createdAt: string
  updatedAt: string
}

export interface SecretMeta {
  id: string
  name: string
  createdAt: string
  updatedAt: string
}

export interface DeployResponse {
  deploymentId: string
  jobId: string
  status: string
}

export interface DashboardStats {
  projects: number
  applications: number
  hosts: number
  deployments: number
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface ContainerSnapshot {
  id: string
  name: string
  image: string
  state: string
  status: string
  ports: string
  labels: string
  grafanaLogsUrl: string
}

export interface ContainerLogs {
  containerRef: string
  logs: string
}

export interface ObservabilitySettings {
  grafanaBaseUrl: string
  lokiBaseUrl: string
  configured: boolean
  hostMetricsUrl: string
}

export interface Pipeline {
  id: string
  projectId: string
  name: string
  serviceId: string
  hostId: string
  webhookToken: string
  createdAt: string
  updatedAt: string
}

export interface PipelineRun {
  id: string
  pipelineId: string
  status: PipelineRunStatus
  triggeredBy: string
  deploymentId: string | null
  jobId: string | null
  startedAt: string | null
  finishedAt: string | null
  lastError: string | null
  createdAt: string
  updatedAt: string
}

export type ProjectMemberRole = 'VIEWER' | 'DEVELOPER' | 'OPERATOR'

export interface ProjectMembership {
  id: string
  projectId: string
  userId: string
  role: ProjectMemberRole
  createdAt: string
  updatedAt: string
}

export type DomainStatus = 'PENDING_DNS' | 'ACTIVE' | 'ERROR'

export interface DomainRecord {
  id: string
  projectId: string
  serviceId: string | null
  hostname: string
  status: DomainStatus
  verificationToken: string
  dnsTxtName: string
  dnsTxtValue: string
  certificateIssuer: string | null
  certificateExpiresAt: string | null
  certificateSans: string | null
  verifiedAt: string | null
  lastError: string | null
  createdAt: string
  updatedAt: string
}

export interface TraefikMetadata {
  routerName: string
  rule: string
  entryPoints: string
  tls: boolean
  certResolver: string
  labels: Record<string, string>
}

export interface AuditEntry {
  id: string
  actorUserId: string | null
  actorUsername: string
  action: string
  resourceType: string
  resourceId: string | null
  metadata: string
  createdAt: string
}

export type NotificationChannelType = 'WEBHOOK' | 'EMAIL'
export type AlertEventType = 'DEPLOY_FAILED' | 'JOB_FAILED'
export type AlertRuleStatus = 'OK' | 'PENDING' | 'FIRING' | 'SILENCED'

export interface NotificationChannel {
  id: string
  name: string
  type: NotificationChannelType
  target: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface AlertRule {
  id: string
  name: string
  eventType: AlertEventType
  projectId: string | null
  channelId: string
  status: AlertRuleStatus
  lastFiredAt: string | null
  lastError: string | null
  createdAt: string
  updatedAt: string
}

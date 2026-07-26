export type Role = 'ADMIN' | 'OPERATOR'

export type ApplicationStatus =
  | 'REGISTERED'
  | 'READY'
  | 'DEPLOYING'
  | 'RUNNING'
  | 'STOPPED'
  | 'FAILED'

export type DeploymentStatus =
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
  createdAt: string
  updatedAt: string
}

export interface Deployment {
  id: string
  applicationId: string
  hostId: string
  status: DeploymentStatus
  startedAt: string | null
  finishedAt: string | null
  logs: string
  createdAt: string
  updatedAt: string
}

export interface DashboardStats {
  applications: number
  hosts: number
  deployments: number
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
}

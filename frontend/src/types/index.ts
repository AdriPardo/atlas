export type ApplicationStatus =
  | 'DRAFT'
  | 'READY'
  | 'DEPLOYING'
  | 'RUNNING'
  | 'FAILED'
  | 'STOPPED';

export type DeploymentStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'CANCELLED';

export type Role = 'ADMIN' | 'OPERATOR';

export interface Application {
  id: string;
  name: string;
  description: string | null;
  repositoryUrl: string;
  branch: string;
  composePath: string;
  domain: string | null;
  status: ApplicationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Host {
  id: string;
  hostname: string;
  ip: string;
  operatingSystem: string;
  dockerVersion: string;
  online: boolean;
  createdAt: string;
}

export interface Deployment {
  id: string;
  applicationId: string;
  hostId: string;
  status: DeploymentStatus;
  startedAt: string;
  finishedAt: string | null;
  logs: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: string;
  username: string;
  role: Role;
}

export interface User {
  id: string;
  username: string;
  role: Role;
  installationId: string;
}

export interface DashboardStats {
  applications: number;
  runningApplications: number;
  hosts: number;
  deployments: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface CreateApplicationRequest {
  name: string;
  description?: string;
  repositoryUrl: string;
  branch?: string;
  composePath?: string;
  domain?: string;
}

export interface UpdateApplicationRequest {
  name: string;
  description?: string;
  repositoryUrl: string;
  branch: string;
  composePath: string;
  domain?: string;
  status: ApplicationStatus;
}

export interface ListApplicationsParams {
  name?: string;
  status?: ApplicationStatus;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface ListHostsParams {
  hostname?: string;
  online?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface ListDeploymentsParams {
  applicationId?: string;
  hostId?: string;
  status?: DeploymentStatus;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

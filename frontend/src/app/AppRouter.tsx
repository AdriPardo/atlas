import { BrowserRouter, Navigate, Route, Routes, useParams } from 'react-router-dom'
import { AppLayout } from '../shared/layout/AppLayout'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import { LoginPage } from '../features/auth/LoginPage'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { ProjectsListPage } from '../features/projects/ProjectsListPage'
import { ProjectDetailPage } from '../features/projects/ProjectDetailPage'
import { ProjectFormPage } from '../features/projects/ProjectFormPage'
import { HostsListPage } from '../features/hosts/HostsListPage'
import { HostDetailPage } from '../features/hosts/HostDetailPage'
import { HostFormPage } from '../features/hosts/HostFormPage'
import { DeploymentsListPage } from '../features/deployments/DeploymentsListPage'
import { DeploymentDetailPage } from '../features/deployments/DeploymentDetailPage'
import { DeploymentFormPage } from '../features/deployments/DeploymentFormPage'
import { PipelinesListPage } from '../features/pipelines/PipelinesListPage'
import { PipelineDetailPage } from '../features/pipelines/PipelineDetailPage'
import { PipelineFormPage } from '../features/pipelines/PipelineFormPage'
import { AuditListPage } from '../features/audit/AuditListPage'
import { BillingPage } from '../features/billing/BillingPage'
import { AlertsPage } from '../features/alerts/AlertsPage'
import { CronJobsPage } from '../features/cron/CronJobsPage'
import { SecretsListPage } from '../features/secrets/SecretsListPage'
import { ProfilePage } from '../features/profile/ProfilePage'

interface AppRouterProps {
  mode: 'light' | 'dark'
  onToggleMode: () => void
}

function RedirectApplicationsToProjects({ suffix = '' }: { suffix?: string }) {
  const { id } = useParams()
  return <Navigate to={`/projects/${id}${suffix}`} replace />
}

export function AppRouter({ mode, onToggleMode }: AppRouterProps) {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage mode={mode} onToggleMode={onToggleMode} />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout mode={mode} onToggleMode={onToggleMode} />}>
            <Route index element={<DashboardPage />} />
            <Route path="projects" element={<ProjectsListPage />} />
            <Route path="projects/new" element={<ProjectFormPage />} />
            <Route path="projects/:id" element={<ProjectDetailPage />} />
            <Route path="projects/:id/edit" element={<ProjectFormPage />} />
            <Route path="applications" element={<Navigate to="/projects" replace />} />
            <Route path="applications/new" element={<Navigate to="/projects/new" replace />} />
            <Route path="applications/:id" element={<RedirectApplicationsToProjects />} />
            <Route path="applications/:id/edit" element={<RedirectApplicationsToProjects suffix="/edit" />} />
            <Route path="hosts" element={<HostsListPage />} />
            <Route path="hosts/new" element={<HostFormPage />} />
            <Route path="hosts/:id" element={<HostDetailPage />} />
            <Route path="hosts/:id/edit" element={<HostFormPage />} />
            <Route path="deployments" element={<DeploymentsListPage />} />
            <Route path="deployments/new" element={<DeploymentFormPage />} />
            <Route path="deployments/:id" element={<DeploymentDetailPage />} />
            <Route path="pipelines" element={<PipelinesListPage />} />
            <Route path="pipelines/new" element={<PipelineFormPage />} />
            <Route path="pipelines/:id" element={<PipelineDetailPage />} />
            <Route path="audit" element={<AuditListPage />} />
            <Route path="billing" element={<BillingPage />} />
            <Route path="alerts" element={<AlertsPage />} />
            <Route path="cron" element={<CronJobsPage />} />
            <Route path="secrets" element={<SecretsListPage />} />
            <Route path="profile" element={<ProfilePage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from '../shared/layout/AppLayout'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import { LoginPage } from '../features/auth/LoginPage'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { ApplicationsListPage } from '../features/applications/ApplicationsListPage'
import { ApplicationDetailPage } from '../features/applications/ApplicationDetailPage'
import { ApplicationFormPage } from '../features/applications/ApplicationFormPage'
import { HostsListPage } from '../features/hosts/HostsListPage'
import { HostDetailPage } from '../features/hosts/HostDetailPage'
import { HostFormPage } from '../features/hosts/HostFormPage'
import { DeploymentsListPage } from '../features/deployments/DeploymentsListPage'
import { DeploymentDetailPage } from '../features/deployments/DeploymentDetailPage'
import { DeploymentFormPage } from '../features/deployments/DeploymentFormPage'
import { ProfilePage } from '../features/profile/ProfilePage'

interface AppRouterProps {
  mode: 'light' | 'dark'
  onToggleMode: () => void
}

export function AppRouter({ mode, onToggleMode }: AppRouterProps) {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage mode={mode} onToggleMode={onToggleMode} />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout mode={mode} onToggleMode={onToggleMode} />}>
            <Route index element={<DashboardPage />} />
            <Route path="applications" element={<ApplicationsListPage />} />
            <Route path="applications/new" element={<ApplicationFormPage />} />
            <Route path="applications/:id" element={<ApplicationDetailPage />} />
            <Route path="applications/:id/edit" element={<ApplicationFormPage />} />
            <Route path="hosts" element={<HostsListPage />} />
            <Route path="hosts/new" element={<HostFormPage />} />
            <Route path="hosts/:id" element={<HostDetailPage />} />
            <Route path="hosts/:id/edit" element={<HostFormPage />} />
            <Route path="deployments" element={<DeploymentsListPage />} />
            <Route path="deployments/new" element={<DeploymentFormPage />} />
            <Route path="deployments/:id" element={<DeploymentDetailPage />} />
            <Route path="profile" element={<ProfilePage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

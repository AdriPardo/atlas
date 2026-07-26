import type { ReactNode } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import { AppShell } from '../components/layout/AppShell';
import { ApplicationDetailPage } from '../pages/applications/ApplicationDetailPage';
import {
  ApplicationCreatePage,
  ApplicationEditPage,
} from '../pages/applications/ApplicationFormPage';
import { ApplicationsListPage } from '../pages/applications/ApplicationsListPage';
import { DashboardPage } from '../pages/DashboardPage';
import { DeploymentsListPage } from '../pages/deployments/DeploymentsListPage';
import { HostDetailPage } from '../pages/hosts/HostDetailPage';
import { HostsListPage } from '../pages/hosts/HostsListPage';
import { LoginPage } from '../pages/LoginPage';
import { ProfilePage } from '../pages/ProfilePage';

function ProtectedLayout({ children }: { children: ReactNode }) {
  return (
    <ProtectedRoute>
      <AppShell>{children}</AppShell>
    </ProtectedRoute>
  );
}

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <ProtectedLayout>
              <DashboardPage />
            </ProtectedLayout>
          }
        />
        <Route
          path="/applications"
          element={
            <ProtectedLayout>
              <ApplicationsListPage />
            </ProtectedLayout>
          }
        />
        <Route
          path="/applications/new"
          element={
            <ProtectedLayout>
              <ApplicationCreatePage />
            </ProtectedLayout>
          }
        />
        <Route
          path="/applications/:id"
          element={
            <ProtectedLayout>
              <ApplicationDetailPage />
            </ProtectedLayout>
          }
        />
        <Route
          path="/applications/:id/edit"
          element={
            <ProtectedLayout>
              <ApplicationEditPage />
            </ProtectedLayout>
          }
        />
        <Route
          path="/hosts"
          element={
            <ProtectedLayout>
              <HostsListPage />
            </ProtectedLayout>
          }
        />
        <Route
          path="/hosts/:id"
          element={
            <ProtectedLayout>
              <HostDetailPage />
            </ProtectedLayout>
          }
        />
        <Route
          path="/deployments"
          element={
            <ProtectedLayout>
              <DeploymentsListPage />
            </ProtectedLayout>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedLayout>
              <ProfilePage />
            </ProtectedLayout>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

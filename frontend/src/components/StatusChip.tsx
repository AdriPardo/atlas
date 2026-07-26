import { Chip } from '@mui/material';
import type { ApplicationStatus, DeploymentStatus } from '../types';

const applicationLabels: Record<ApplicationStatus, string> = {
  DRAFT: 'Borrador',
  READY: 'Lista',
  DEPLOYING: 'Desplegando',
  RUNNING: 'En ejecución',
  FAILED: 'Fallida',
  STOPPED: 'Detenida',
};

const applicationColors: Record<
  ApplicationStatus,
  'default' | 'info' | 'warning' | 'success' | 'error' | 'secondary'
> = {
  DRAFT: 'default',
  READY: 'info',
  DEPLOYING: 'warning',
  RUNNING: 'success',
  FAILED: 'error',
  STOPPED: 'secondary',
};

const deploymentLabels: Record<DeploymentStatus, string> = {
  PENDING: 'Pendiente',
  RUNNING: 'En curso',
  SUCCEEDED: 'Exitoso',
  FAILED: 'Fallido',
  CANCELLED: 'Cancelado',
};

const deploymentColors: Record<
  DeploymentStatus,
  'default' | 'info' | 'warning' | 'success' | 'error'
> = {
  PENDING: 'default',
  RUNNING: 'info',
  SUCCEEDED: 'success',
  FAILED: 'error',
  CANCELLED: 'warning',
};

export function ApplicationStatusChip({ status }: { status: ApplicationStatus }) {
  return (
    <Chip
      size="small"
      label={applicationLabels[status]}
      color={applicationColors[status]}
      variant="outlined"
    />
  );
}

export function DeploymentStatusChip({ status }: { status: DeploymentStatus }) {
  return (
    <Chip
      size="small"
      label={deploymentLabels[status]}
      color={deploymentColors[status]}
      variant="outlined"
    />
  );
}

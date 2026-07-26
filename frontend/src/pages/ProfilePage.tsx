import { Chip, Divider, Paper, Typography, Box, Button } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import {
  ErrorState,
  LoadingState,
  PageHeader,
  getErrorMessage,
} from '../components/PageHelpers';

function DetailRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <Box sx={{ py: 1.5 }}>
      <Typography variant="caption" color="text.secondary" display="block">
        {label}
      </Typography>
      <Typography variant="body1" sx={{ wordBreak: 'break-word' }}>
        {value || '—'}
      </Typography>
    </Box>
  );
}

export function ProfilePage() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: getCurrentUser,
  });

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  if (isLoading) return <LoadingState label="Cargando perfil…" />;
  if (isError || !data) {
    return (
      <ErrorState
        message={getErrorMessage(error, 'No se pudo cargar el perfil.')}
        action={
          <Button color="inherit" size="small" onClick={() => refetch()}>
            Reintentar
          </Button>
        }
      />
    );
  }

  return (
    <>
      <PageHeader
        title="Perfil"
        subtitle="Información de la sesión actual."
        actions={
          <Button color="error" variant="outlined" onClick={handleLogout}>
            Cerrar sesión
          </Button>
        }
      />
      <Paper sx={{ p: 3, maxWidth: 560 }}>
        <DetailRow label="Usuario" value={data.username} />
        <Divider />
        <DetailRow
          label="Rol"
          value={<Chip size="small" label={data.role} color="primary" variant="outlined" />}
        />
        <Divider />
        <DetailRow label="ID de usuario" value={data.id} />
        <Divider />
        <DetailRow label="ID de instalación" value={data.installationId} />
      </Paper>
    </>
  );
}

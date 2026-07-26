import { Box, Button, Chip, Divider, Paper, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import { getHost } from '../../api/hosts';
import {
  ErrorState,
  LoadingState,
  PageHeader,
  formatDateTime,
  getErrorMessage,
} from '../../components/PageHelpers';

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

export function HostDetailPage() {
  const { id = '' } = useParams();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['hosts', id],
    queryFn: () => getHost(id),
    enabled: Boolean(id),
  });

  if (isLoading) return <LoadingState label="Cargando host…" />;
  if (isError || !data) {
    return (
      <ErrorState
        message={getErrorMessage(error, 'No se pudo cargar el host.')}
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
        title={data.hostname}
        subtitle="Detalle del host"
        actions={
          <Button component={RouterLink} to="/hosts" variant="outlined">
            Volver
          </Button>
        }
      />
      <Paper sx={{ p: 3 }}>
        <DetailRow
          label="Estado"
          value={
            <Chip
              size="small"
              label={data.online ? 'En línea' : 'Fuera de línea'}
              color={data.online ? 'success' : 'default'}
              variant="outlined"
            />
          }
        />
        <Divider />
        <DetailRow label="Dirección IP" value={data.ip} />
        <Divider />
        <DetailRow label="Sistema operativo" value={data.operatingSystem} />
        <Divider />
        <DetailRow label="Versión Docker" value={data.dockerVersion} />
        <Divider />
        <DetailRow label="Creado" value={formatDateTime(data.createdAt)} />
      </Paper>
    </>
  );
}

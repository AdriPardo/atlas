import { Delete as DeleteIcon, Edit as EditIcon } from '@mui/icons-material';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { deleteApplication, getApplication } from '../../api/applications';
import {
  ErrorState,
  LoadingState,
  PageHeader,
  formatDateTime,
  getErrorMessage,
} from '../../components/PageHelpers';
import { ApplicationStatusChip } from '../../components/StatusChip';

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

export function ApplicationDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['applications', id],
    queryFn: () => getApplication(id),
    enabled: Boolean(id),
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteApplication(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['applications'] });
      navigate('/applications');
    },
  });

  if (isLoading) return <LoadingState label="Cargando aplicación…" />;
  if (isError || !data) {
    return (
      <ErrorState
        message={getErrorMessage(error, 'No se pudo cargar la aplicación.')}
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
        title={data.name}
        subtitle="Detalle de la aplicación"
        actions={
          <>
            <Button
              variant="outlined"
              startIcon={<EditIcon />}
              component={RouterLink}
              to={`/applications/${data.id}/edit`}
            >
              Editar
            </Button>
            <Button
              variant="outlined"
              color="error"
              startIcon={<DeleteIcon />}
              onClick={() => setConfirmOpen(true)}
            >
              Eliminar
            </Button>
          </>
        }
      />

      <Paper sx={{ p: 3 }}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent="space-between"
          alignItems={{ sm: 'center' }}
          spacing={1}
          sx={{ mb: 1 }}
        >
          <Typography variant="h6">Información general</Typography>
          <ApplicationStatusChip status={data.status} />
        </Stack>
        <Divider />
        <DetailRow label="Descripción" value={data.description} />
        <Divider />
        <DetailRow label="Repositorio" value={data.repositoryUrl} />
        <Divider />
        <DetailRow label="Rama" value={data.branch} />
        <Divider />
        <DetailRow label="Ruta Compose" value={data.composePath} />
        <Divider />
        <DetailRow label="Dominio" value={data.domain} />
        <Divider />
        <DetailRow label="Creado" value={formatDateTime(data.createdAt)} />
        <Divider />
        <DetailRow label="Actualizado" value={formatDateTime(data.updatedAt)} />
      </Paper>

      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle>Eliminar aplicación</DialogTitle>
        <DialogContent>
          <DialogContentText>
            ¿Seguro que deseas eliminar <strong>{data.name}</strong>? Esta acción no se puede
            deshacer.
          </DialogContentText>
          {deleteMutation.isError && (
            <Typography color="error" variant="body2" sx={{ mt: 2 }}>
              {getErrorMessage(deleteMutation.error, 'No se pudo eliminar la aplicación.')}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Cancelar</Button>
          <Button
            color="error"
            variant="contained"
            disabled={deleteMutation.isPending}
            onClick={() => deleteMutation.mutate()}
          >
            Eliminar
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

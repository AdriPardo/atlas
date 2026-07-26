import {
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { listDeployments } from '../../api/deployments';
import { ErrorState, LoadingState, PageHeader, formatDateTime } from '../../components/PageHelpers';
import { DeploymentStatusChip } from '../../components/StatusChip';
import type { DeploymentStatus } from '../../types';

const STATUS_OPTIONS: Array<{ value: '' | DeploymentStatus; label: string }> = [
  { value: '', label: 'Todos' },
  { value: 'PENDING', label: 'Pendiente' },
  { value: 'RUNNING', label: 'En curso' },
  { value: 'SUCCEEDED', label: 'Exitoso' },
  { value: 'FAILED', label: 'Fallido' },
  { value: 'CANCELLED', label: 'Cancelado' },
];

export function DeploymentsListPage() {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [status, setStatus] = useState<'' | DeploymentStatus>('');

  const params = useMemo(
    () => ({
      page,
      size: rowsPerPage,
      status: status || undefined,
      sortBy: 'startedAt',
      sortDir: 'desc' as const,
    }),
    [page, rowsPerPage, status],
  );

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['deployments', params],
    queryFn: () => listDeployments(params),
  });

  return (
    <>
      <PageHeader
        title="Despliegues"
        subtitle="Historial de despliegues en la plataforma."
      />

      <Paper sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel id="deployment-status-label">Estado</InputLabel>
            <Select
              labelId="deployment-status-label"
              label="Estado"
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value as '' | DeploymentStatus);
              }}
            >
              {STATUS_OPTIONS.map((option) => (
                <MenuItem key={option.value || 'all'} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>
      </Paper>

      {isLoading ? (
        <LoadingState label="Cargando despliegues…" />
      ) : isError ? (
        <ErrorState
          message={
            error instanceof Error ? error.message : 'No se pudieron cargar los despliegues.'
          }
          action={
            <Button color="inherit" size="small" onClick={() => refetch()}>
              Reintentar
            </Button>
          }
        />
      ) : (
        <Paper>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Aplicación</TableCell>
                  <TableCell>Host</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell>Inicio</TableCell>
                  <TableCell>Fin</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(data?.content ?? []).length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6}>
                      <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
                        No hay despliegues registrados.
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  data?.content.map((deployment) => (
                    <TableRow
                      key={deployment.id}
                      hover
                      component={RouterLink}
                      to={`/deployments/${deployment.id}`}
                      sx={{ textDecoration: 'none', cursor: 'pointer' }}
                    >
                      <TableCell>
                        <Typography variant="body2" fontFamily="monospace">
                          {deployment.id.slice(0, 8)}…
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontFamily="monospace">
                          {deployment.applicationId.slice(0, 8)}…
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontFamily="monospace">
                          {deployment.hostId.slice(0, 8)}…
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <DeploymentStatusChip status={deployment.status} />
                      </TableCell>
                      <TableCell>{formatDateTime(deployment.startedAt)}</TableCell>
                      <TableCell>{formatDateTime(deployment.finishedAt)}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
          <TablePagination
            component="div"
            count={data?.totalElements ?? 0}
            page={page}
            onPageChange={(_, newPage) => setPage(newPage)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => {
              setRowsPerPage(parseInt(e.target.value, 10));
              setPage(0);
            }}
            rowsPerPageOptions={[5, 10, 20, 50]}
            labelRowsPerPage="Filas"
            labelDisplayedRows={({ from, to, count }) =>
              `${from}–${to} de ${count !== -1 ? count : `más de ${to}`}`
            }
            disabled={isFetching}
          />
        </Paper>
      )}
    </>
  );
}

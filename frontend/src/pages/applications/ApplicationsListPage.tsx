import { Add as AddIcon } from '@mui/icons-material';
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
  TextField,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { listApplications } from '../../api/applications';
import { ErrorState, LoadingState, PageHeader, formatDateTime } from '../../components/PageHelpers';
import { ApplicationStatusChip } from '../../components/StatusChip';
import type { ApplicationStatus } from '../../types';

const STATUS_OPTIONS: Array<{ value: '' | ApplicationStatus; label: string }> = [
  { value: '', label: 'Todos' },
  { value: 'DRAFT', label: 'Borrador' },
  { value: 'READY', label: 'Lista' },
  { value: 'DEPLOYING', label: 'Desplegando' },
  { value: 'RUNNING', label: 'En ejecución' },
  { value: 'FAILED', label: 'Fallida' },
  { value: 'STOPPED', label: 'Detenida' },
];

export function ApplicationsListPage() {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [nameInput, setNameInput] = useState('');
  const [name, setName] = useState('');
  const [status, setStatus] = useState<'' | ApplicationStatus>('');

  const params = useMemo(
    () => ({
      page,
      size: rowsPerPage,
      name: name || undefined,
      status: status || undefined,
      sortBy: 'createdAt',
      sortDir: 'desc' as const,
    }),
    [page, rowsPerPage, name, status],
  );

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['applications', params],
    queryFn: () => listApplications(params),
  });

  const handleSearch = () => {
    setPage(0);
    setName(nameInput.trim());
  };

  return (
    <>
      <PageHeader
        title="Aplicaciones"
        subtitle="Gestiona las aplicaciones desplegables en Atlas."
        actions={
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            component={RouterLink}
            to="/applications/new"
          >
            Nueva aplicación
          </Button>
        }
      />

      <Paper sx={{ p: 2, mb: 2 }}>
        <Box
          sx={{
            display: 'flex',
            flexWrap: 'wrap',
            gap: 2,
            alignItems: 'center',
          }}
        >
          <TextField
            label="Buscar por nombre"
            size="small"
            value={nameInput}
            onChange={(e) => setNameInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch();
            }}
            sx={{ minWidth: 220, flex: 1 }}
          />
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel id="status-filter-label">Estado</InputLabel>
            <Select
              labelId="status-filter-label"
              label="Estado"
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value as '' | ApplicationStatus);
              }}
            >
              {STATUS_OPTIONS.map((option) => (
                <MenuItem key={option.value || 'all'} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Button variant="outlined" onClick={handleSearch}>
            Buscar
          </Button>
        </Box>
      </Paper>

      {isLoading ? (
        <LoadingState label="Cargando aplicaciones…" />
      ) : isError ? (
        <ErrorState
          message={error instanceof Error ? error.message : 'No se pudieron cargar las aplicaciones.'}
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
                  <TableCell>Nombre</TableCell>
                  <TableCell>Dominio</TableCell>
                  <TableCell>Rama</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell>Actualizado</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(data?.content ?? []).length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5}>
                      <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
                        No hay aplicaciones que coincidan con los filtros.
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  data?.content.map((app) => (
                    <TableRow
                      key={app.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      component={RouterLink}
                      to={`/applications/${app.id}`}
                      style={{ textDecoration: 'none' }}
                    >
                      <TableCell>
                        <Typography fontWeight={600}>{app.name}</Typography>
                        <Typography variant="caption" color="text.secondary" noWrap>
                          {app.repositoryUrl}
                        </Typography>
                      </TableCell>
                      <TableCell>{app.domain || '—'}</TableCell>
                      <TableCell>{app.branch}</TableCell>
                      <TableCell>
                        <ApplicationStatusChip status={app.status} />
                      </TableCell>
                      <TableCell>{formatDateTime(app.updatedAt)}</TableCell>
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

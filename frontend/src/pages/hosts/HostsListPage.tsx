import {
  Chip,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
  Box,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { listHosts } from '../../api/hosts';
import { ErrorState, LoadingState, PageHeader, formatDateTime } from '../../components/PageHelpers';

export function HostsListPage() {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [hostnameInput, setHostnameInput] = useState('');
  const [hostname, setHostname] = useState('');
  const [online, setOnline] = useState<'' | 'true' | 'false'>('');

  const params = useMemo(
    () => ({
      page,
      size: rowsPerPage,
      hostname: hostname || undefined,
      online: online === '' ? undefined : online === 'true',
      sortBy: 'createdAt',
      sortDir: 'desc' as const,
    }),
    [page, rowsPerPage, hostname, online],
  );

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ['hosts', params],
    queryFn: () => listHosts(params),
  });

  const handleSearch = () => {
    setPage(0);
    setHostname(hostnameInput.trim());
  };

  return (
    <>
      <PageHeader
        title="Hosts"
        subtitle="Servidores disponibles para desplegar aplicaciones."
      />

      <Paper sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center' }}>
          <TextField
            label="Buscar por hostname"
            size="small"
            value={hostnameInput}
            onChange={(e) => setHostnameInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch();
            }}
            sx={{ minWidth: 220, flex: 1 }}
          />
          <FormControl size="small" sx={{ minWidth: 160 }}>
            <InputLabel id="online-filter-label">Estado</InputLabel>
            <Select
              labelId="online-filter-label"
              label="Estado"
              value={online}
              onChange={(e) => {
                setPage(0);
                setOnline(e.target.value as '' | 'true' | 'false');
              }}
            >
              <MenuItem value="">Todos</MenuItem>
              <MenuItem value="true">En línea</MenuItem>
              <MenuItem value="false">Fuera de línea</MenuItem>
            </Select>
          </FormControl>
          <Button variant="outlined" onClick={handleSearch}>
            Buscar
          </Button>
        </Box>
      </Paper>

      {isLoading ? (
        <LoadingState label="Cargando hosts…" />
      ) : isError ? (
        <ErrorState
          message={error instanceof Error ? error.message : 'No se pudieron cargar los hosts.'}
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
                  <TableCell>Hostname</TableCell>
                  <TableCell>IP</TableCell>
                  <TableCell>Sistema</TableCell>
                  <TableCell>Docker</TableCell>
                  <TableCell>Estado</TableCell>
                  <TableCell>Creado</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(data?.content ?? []).length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6}>
                      <Typography color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
                        No hay hosts que coincidan con los filtros.
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  data?.content.map((host) => (
                    <TableRow
                      key={host.id}
                      hover
                      sx={{ cursor: 'pointer', textDecoration: 'none' }}
                      component={RouterLink}
                      to={`/hosts/${host.id}`}
                    >
                      <TableCell>
                        <Typography fontWeight={600}>{host.hostname}</Typography>
                      </TableCell>
                      <TableCell>{host.ip}</TableCell>
                      <TableCell>{host.operatingSystem}</TableCell>
                      <TableCell>{host.dockerVersion}</TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={host.online ? 'En línea' : 'Fuera de línea'}
                          color={host.online ? 'success' : 'default'}
                          variant="outlined"
                        />
                      </TableCell>
                      <TableCell>{formatDateTime(host.createdAt)}</TableCell>
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

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  Link,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { hostsApi, settingsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { LogViewer } from '../../shared/components/LogViewer'
import { RowOverflowMenu } from '../../shared/components/RowOverflowMenu'
import type { ContainerSnapshot } from '../../shared/types/api'

export function HostDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [selected, setSelected] = useState<ContainerSnapshot | null>(null)

  const query = useQuery({
    queryKey: ['hosts', id],
    queryFn: () => hostsApi.get(id),
    enabled: !!id,
  })

  const containersQuery = useQuery({
    queryKey: ['hosts', id, 'containers'],
    queryFn: () => hostsApi.containers(id),
    enabled: !!id,
    refetchInterval: 15_000,
  })

  const obsQuery = useQuery({
    queryKey: ['settings', 'observability'],
    queryFn: () => settingsApi.observability(),
  })

  const logsQuery = useQuery({
    queryKey: ['hosts', id, 'containers', selected?.name ?? selected?.id, 'logs'],
    queryFn: () => hostsApi.containerLogs(id, selected!.name || selected!.id, 200),
    enabled: !!id && !!selected,
  })

  const syncMutation = useMutation({
    mutationFn: () => hostsApi.sync(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['hosts', id] })
      await queryClient.invalidateQueries({ queryKey: ['jobs'] })
    },
  })

  const restartMutation = useMutation({
    mutationFn: (containerRef: string) => hostsApi.restartContainer(id, containerRef),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['hosts', id, 'containers'] })
    },
  })

  const containers = containersQuery.data ?? []
  const hostMetricsUrl = obsQuery.data?.hostMetricsUrl
  const grafanaConfigured = Boolean(obsQuery.data?.configured && obsQuery.data?.grafanaBaseUrl)

  return (
    <PageShell maxWidth={960}>
      <PageHeader
        title={query.data?.hostname ?? 'Host'}
        description="Inventory, containers, and observability links."
        actions={
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center">
            <Button component={RouterLink} to="/hosts">
              Back
            </Button>
            <Button
              variant="outlined"
              onClick={() => syncMutation.mutate()}
              disabled={syncMutation.isPending}
            >
              Sync
            </Button>
            <Button variant="contained" onClick={() => navigate(`/hosts/${id}/edit`)}>
              Edit
            </Button>
            {grafanaConfigured && hostMetricsUrl ? (
              <RowOverflowMenu
                aria-label="More host actions"
                items={[
                  {
                    label: 'Open metrics',
                    onClick: () => window.open(hostMetricsUrl, '_blank', 'noopener,noreferrer'),
                  },
                ]}
              />
            ) : null}
          </Stack>
        }
      />

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <Stack spacing={2.5}>
            {syncMutation.isSuccess && (
              <Alert severity="success" variant="outlined">
                Sync job queued ({syncMutation.data.id.slice(0, 8)}). Refresh shortly for OS / runtime
                metadata.
              </Alert>
            )}
            {syncMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to queue sync job
              </Alert>
            )}
            {restartMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to restart container (runtime may be disabled in this environment).
              </Alert>
            )}
            <DetailPanel>
              <DetailField label="Status">
                <StatusChip label={query.data.online ? 'ONLINE' : 'OFFLINE'} />
              </DetailField>
              <DetailField label="Connection">{query.data.connectionType}</DetailField>
              <DetailField label="IP" mono>
                {query.data.ip}
              </DetailField>
              <DetailField label="SSH user">{query.data.sshUser || '-'}</DetailField>
              <DetailField label="SSH port">{query.data.sshPort}</DetailField>
              <DetailField label="SSH secret" mono>
                {query.data.sshPrivateKeySecretId || '-'}
              </DetailField>
              <DetailField label="Operating system">{query.data.operatingSystem}</DetailField>
              <DetailField label="Runtime version" mono>
                {query.data.dockerVersion || '-'}
              </DetailField>
              <DetailField label="Created">
                {new Date(query.data.createdAt).toLocaleString()}
              </DetailField>
            </DetailPanel>

            <Stack spacing={1}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
                  Containers
                </Typography>
                <Button
                  size="small"
                  onClick={() => containersQuery.refetch()}
                  disabled={containersQuery.isFetching}
                >
                  Refresh
                </Button>
              </Stack>
              <QueryState
                isLoading={containersQuery.isLoading}
                isError={containersQuery.isError}
                errorMessage="Unable to list containers on this host."
              >
                {containers.length === 0 ? (
                  <EmptyState
                    title="No containers"
                    description="Sync the host or deploy a service to discover running workloads here."
                  />
                ) : (
                  <DataTableFrame>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Name</TableCell>
                          <TableCell>Image</TableCell>
                          <TableCell>State</TableCell>
                          <TableCell>Ports</TableCell>
                          <TableCell align="right">Actions</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {containers.map((c) => {
                          const ref = c.name || c.id
                          return (
                            <TableRow key={c.id || c.name} hover selected={selected?.id === c.id}>
                              <TableCell>
                                <Typography variant="body2" className="atlas-mono">
                                  {c.name || c.id.slice(0, 12)}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <Typography variant="body2" color="text.secondary">
                                  {c.image || '-'}
                                </Typography>
                              </TableCell>
                              <TableCell>
                                <StatusChip label={(c.state || c.status || 'unknown').toUpperCase()} />
                              </TableCell>
                              <TableCell>
                                <Typography variant="caption" className="atlas-mono">
                                  {c.ports || '-'}
                                </Typography>
                              </TableCell>
                              <TableCell align="right">
                                <Stack
                                  direction="row"
                                  spacing={0.25}
                                  justifyContent="flex-end"
                                  alignItems="center"
                                >
                                  <Button size="small" onClick={() => setSelected(c)}>
                                    Logs
                                  </Button>
                                  <RowOverflowMenu
                                    aria-label={`More actions for ${c.name || c.id.slice(0, 12)}`}
                                    items={[
                                      ...(c.grafanaLogsUrl
                                        ? [
                                            {
                                              label: 'Open log explorer',
                                              onClick: () =>
                                                window.open(
                                                  c.grafanaLogsUrl!,
                                                  '_blank',
                                                  'noopener,noreferrer',
                                                ),
                                            },
                                          ]
                                        : []),
                                      {
                                        label: 'Restart',
                                        disabled: restartMutation.isPending,
                                        onClick: () => restartMutation.mutate(ref),
                                      },
                                    ]}
                                  />
                                </Stack>
                              </TableCell>
                            </TableRow>
                          )
                        })}
                      </TableBody>
                    </Table>
                  </DataTableFrame>
                )}
              </QueryState>
            </Stack>

            {selected && (
              <Stack spacing={1}>
                <Stack direction="row" justifyContent="space-between" alignItems="center">
                  <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
                    Runtime logs — {selected.name || selected.id.slice(0, 12)}
                  </Typography>
                  <Link
                    component="button"
                    variant="body2"
                    onClick={() => setSelected(null)}
                    underline="hover"
                  >
                    Close
                  </Link>
                </Stack>
                <QueryState
                  isLoading={logsQuery.isLoading}
                  isError={logsQuery.isError}
                  errorMessage="Unable to fetch runtime logs (runtime may be disabled in this environment)."
                >
                  <LogViewer logs={logsQuery.data?.logs ?? ''} maxHeight={360} />
                </QueryState>
              </Stack>
            )}
          </Stack>
        )}
      </QueryState>
    </PageShell>
  )
}

import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import {
  Box,
  Button,
  Chip,
  Link,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RocketLaunchOutlinedIcon from '@mui/icons-material/RocketLaunchOutlined'
import { deploymentsApi } from '../../shared/api/endpoints'
import type { DeploymentStatus } from '../../shared/types/api'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { StatusChip } from '../../shared/components/StatusChip'

const STATUS_FILTERS: Array<{ label: string; value: DeploymentStatus | 'ALL' }> = [
  { label: 'All', value: 'ALL' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Running', value: 'RUNNING' },
  { label: 'Succeeded', value: 'SUCCEEDED' },
  { label: 'Failed', value: 'FAILED' },
]

export function DeploymentsListPage() {
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<DeploymentStatus | 'ALL'>('ALL')
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['deployments'],
    queryFn: () => deploymentsApi.list({ page: 0, size: 50, sort: 'createdAt,desc' }),
  })

  const removeMutation = useMutation({
    mutationFn: (id: string) => deploymentsApi.remove(id),
    onSuccess: async () => {
      setDeleteId(null)
      await queryClient.invalidateQueries({ queryKey: ['deployments'] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
    },
  })

  const rows = useMemo(() => {
    const all = query.data?.content ?? []
    if (statusFilter === 'ALL') return all
    return all.filter((item) => item.status === statusFilter)
  }, [query.data?.content, statusFilter])

  return (
    <PageShell>
      <PageHeader
        title="Deployments"
        description="Track deploy runs from project services to hosts."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/deployments/new')}>
            New deployment
          </Button>
        }
      />

      <DataTableFrame
        toolbar={
          <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
            {STATUS_FILTERS.map((filter) => {
              const selected = statusFilter === filter.value
              return (
                <Chip
                  key={filter.value}
                  size="small"
                  label={filter.label}
                  color={selected ? 'primary' : 'default'}
                  variant={selected ? 'filled' : 'outlined'}
                  onClick={() => setStatusFilter(filter.value)}
                  sx={{ fontWeight: selected ? 650 : 500 }}
                />
              )
            })}
          </Stack>
        }
      >
        <QueryState
          isLoading={query.isLoading}
          isError={query.isError}
          onRetry={() => query.refetch()}
          skeleton="table"
          errorMessage="Could not load deployments."
        >
          {rows.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<RocketLaunchOutlinedIcon />}
                title={statusFilter === 'ALL' ? 'No deployments yet' : 'No matches'}
                description={
                  statusFilter === 'ALL'
                    ? 'Create a deployment to link a service with a host, or deploy from a project.'
                    : 'No deployments with this status. Try another filter.'
                }
                actionLabel={statusFilter === 'ALL' ? 'New deployment' : 'Clear filter'}
                onAction={
                  statusFilter === 'ALL'
                    ? () => navigate('/deployments/new')
                    : () => setStatusFilter('ALL')
                }
              />
            </Box>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Service</TableCell>
                  <TableCell>Host</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((item) => (
                  <TableRow key={item.id} hover>
                    <TableCell>
                      <Link
                        component={RouterLink}
                        to={`/deployments/${item.id}`}
                        className="atlas-mono"
                        underline="hover"
                        sx={{ fontWeight: 600 }}
                      >
                        {item.id.slice(0, 8)}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" className="atlas-mono" color="text.secondary">
                        {(item.serviceId ?? item.applicationId ?? '').slice(0, 8)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Link
                        component={RouterLink}
                        to={`/hosts/${item.hostId}`}
                        className="atlas-mono"
                        underline="hover"
                      >
                        {item.hostId.slice(0, 8)}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <StatusChip label={item.status} />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {new Date(item.createdAt).toLocaleString()}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Button size="small" color="error" onClick={() => setDeleteId(item.id)}>
                        Delete
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </QueryState>
      </DataTableFrame>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete deployment"
        description="This action cannot be undone."
        loading={removeMutation.isPending}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId && removeMutation.mutate(deleteId)}
      />
    </PageShell>
  )
}

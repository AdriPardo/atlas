import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import {
  Box,
  Button,
  Link,
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
import { ConfirmDialog } from '../../shared/components/ConfirmDialog'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { StatusChip } from '../../shared/components/StatusChip'

export function DeploymentsListPage() {
  const [deleteId, setDeleteId] = useState<string | null>(null)
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

  const rows = query.data?.content ?? []

  return (
    <PageShell>
      <PageHeader
        title="Deployments"
        description="Manual deployment records. Runtime execution is not enabled in the MVP."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/deployments/new')}>
            New deployment
          </Button>
        }
      />

      <DataTableFrame>
        <QueryState isLoading={query.isLoading} isError={query.isError}>
          {rows.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<RocketLaunchOutlinedIcon />}
                title="No deployments yet"
                description="Create a deployment to link an application with a host."
                actionLabel="New deployment"
                onAction={() => navigate('/deployments/new')}
              />
            </Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Application</TableCell>
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
                      >
                        {item.id.slice(0, 8)}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Link
                        component={RouterLink}
                        to={`/applications/${item.applicationId}`}
                        className="atlas-mono"
                        underline="hover"
                      >
                        {item.applicationId.slice(0, 8)}
                      </Link>
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

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
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import AppsOutlinedIcon from '@mui/icons-material/AppsOutlined'
import { applicationsApi } from '../../shared/api/endpoints'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { StatusChip } from '../../shared/components/StatusChip'

export function ApplicationsListPage() {
  const [name, setName] = useState('')
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['applications', name],
    queryFn: () => applicationsApi.list({ name: name || undefined, page: 0, size: 50 }),
  })

  const removeMutation = useMutation({
    mutationFn: (id: string) => applicationsApi.remove(id),
    onSuccess: async () => {
      setDeleteId(null)
      setDeleteError(null)
      await queryClient.invalidateQueries({ queryKey: ['applications'] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
    },
    onError: () => setDeleteError('Unable to delete application. It may have deployments.'),
  })

  const rows = query.data?.content ?? []

  return (
    <PageShell>
      <PageHeader
        title="Applications"
        description="Register and manage application definitions."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/applications/new')}>
            New application
          </Button>
        }
      />

      <DataTableFrame
        toolbar={
          <TextField
            label="Filter by name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            sx={{ maxWidth: 320, width: '100%' }}
          />
        }
      >
        <QueryState isLoading={query.isLoading} isError={query.isError}>
          {rows.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<AppsOutlinedIcon />}
                title={name ? 'No matches' : 'No applications yet'}
                description={
                  name
                    ? 'Try a different name filter.'
                    : 'Create an application to start tracking repositories and compose paths.'
                }
                actionLabel={name ? undefined : 'New application'}
                onAction={name ? undefined : () => navigate('/applications/new')}
              />
            </Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Branch</TableCell>
                  <TableCell>Domain</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((app) => (
                  <TableRow key={app.id} hover>
                    <TableCell>
                      <Link component={RouterLink} to={`/applications/${app.id}`} underline="hover" sx={{ fontWeight: 600 }}>
                        {app.name}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <StatusChip label={app.status} />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" className="atlas-mono" color="text.secondary">
                        {app.branch}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {app.domain || '-'}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Button size="small" onClick={() => navigate(`/applications/${app.id}/edit`)}>
                        Edit
                      </Button>
                      <Button size="small" color="error" onClick={() => setDeleteId(app.id)}>
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
        title="Delete application"
        description="This action cannot be undone."
        error={deleteError}
        loading={removeMutation.isPending}
        onClose={() => {
          setDeleteId(null)
          setDeleteError(null)
        }}
        onConfirm={() => deleteId && removeMutation.mutate(deleteId)}
      />
    </PageShell>
  )
}

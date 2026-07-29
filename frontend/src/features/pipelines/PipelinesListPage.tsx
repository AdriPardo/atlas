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
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined'
import { pipelinesApi } from '../../shared/api/endpoints'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { RowOverflowMenu } from '../../shared/components/RowOverflowMenu'

export function PipelinesListPage() {
  const [name, setName] = useState('')
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['pipelines', name],
    queryFn: () => pipelinesApi.list({ name: name || undefined, page: 0, size: 50 }),
  })

  const removeMutation = useMutation({
    mutationFn: (id: string) => pipelinesApi.remove(id),
    onSuccess: async () => {
      setDeleteId(null)
      setDeleteError(null)
      await queryClient.invalidateQueries({ queryKey: ['pipelines'] })
    },
    onError: () => setDeleteError('Unable to delete pipeline.'),
  })

  const rows = query.data?.content ?? []

  return (
    <PageShell>
      <PageHeader
        title="Pipelines"
        description="Deploy-centric pipelines that enqueue deploy jobs on demand."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/pipelines/new')}>
            New pipeline
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
                icon={<AccountTreeOutlinedIcon />}
                title={name ? 'No matches' : 'No pipelines yet'}
                description={
                  name
                    ? 'Try another filter.'
                    : 'Create a pipeline that deploys a service to a host on demand.'
                }
                actionLabel={name ? undefined : 'New pipeline'}
                onAction={name ? undefined : () => navigate('/pipelines/new')}
              />
            </Box>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Project</TableCell>
                  <TableCell>Service</TableCell>
                  <TableCell>Host</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id} hover>
                    <TableCell>
                      <Link component={RouterLink} to={`/pipelines/${row.id}`} underline="hover">
                        {row.name}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" className="atlas-mono">
                        {row.projectId.slice(0, 8)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" className="atlas-mono">
                        {row.serviceId.slice(0, 8)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" className="atlas-mono">
                        {row.hostId ? row.hostId.slice(0, 8) : 'Autopilot'}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <RowOverflowMenu
                        aria-label={`Actions for ${row.name}`}
                        items={[
                          {
                            label: 'Delete',
                            destructive: true,
                            onClick: () => setDeleteId(row.id),
                          },
                        ]}
                      />
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
        title="Delete pipeline?"
        description="Runs history for this pipeline will be removed."
        confirmLabel="Delete"
        error={deleteError}
        onClose={() => {
          setDeleteId(null)
          setDeleteError(null)
        }}
        onConfirm={() => deleteId && removeMutation.mutate(deleteId)}
        loading={removeMutation.isPending}
      />
    </PageShell>
  )
}

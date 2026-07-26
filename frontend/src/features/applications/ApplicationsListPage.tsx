import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import {
  Box,
  Button,
  Chip,
  Link,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import { applicationsApi } from '../../shared/api/endpoints'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog'
import { QueryState } from '../../shared/components/QueryState'

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

  return (
    <Stack spacing={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" gap={2} flexWrap="wrap">
        <Box>
          <Typography variant="h4">Applications</Typography>
          <Typography color="text.secondary">Register and manage application definitions.</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/applications/new')}>
          New application
        </Button>
      </Box>

      <TextField
        label="Filter by name"
        value={name}
        onChange={(e) => setName(e.target.value)}
        sx={{ maxWidth: 360 }}
      />

      <Paper>
        <QueryState isLoading={query.isLoading} isError={query.isError}>
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
              {(query.data?.content ?? []).map((app) => (
                <TableRow key={app.id} hover>
                  <TableCell>
                    <Link component={RouterLink} to={`/applications/${app.id}`}>
                      {app.name}
                    </Link>
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={app.status} />
                  </TableCell>
                  <TableCell>{app.branch}</TableCell>
                  <TableCell>{app.domain || '—'}</TableCell>
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
              {(query.data?.content.length ?? 0) === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>No applications found</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </QueryState>
      </Paper>

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
    </Stack>
  )
}

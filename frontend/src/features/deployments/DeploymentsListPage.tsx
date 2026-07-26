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
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import { deploymentsApi } from '../../shared/api/endpoints'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog'
import { QueryState } from '../../shared/components/QueryState'

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

  return (
    <Stack spacing={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" gap={2} flexWrap="wrap">
        <Box>
          <Typography variant="h4">Deployments</Typography>
          <Typography color="text.secondary">
            Manual deployment records. Runtime execution is not enabled in the MVP.
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/deployments/new')}>
          New deployment
        </Button>
      </Box>

      <Paper>
        <QueryState isLoading={query.isLoading} isError={query.isError}>
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
              {(query.data?.content ?? []).map((item) => (
                <TableRow key={item.id} hover>
                  <TableCell>
                    <Link component={RouterLink} to={`/deployments/${item.id}`}>
                      {item.id.slice(0, 8)}
                    </Link>
                  </TableCell>
                  <TableCell>
                    <Link component={RouterLink} to={`/applications/${item.applicationId}`}>
                      {item.applicationId.slice(0, 8)}
                    </Link>
                  </TableCell>
                  <TableCell>
                    <Link component={RouterLink} to={`/hosts/${item.hostId}`}>
                      {item.hostId.slice(0, 8)}
                    </Link>
                  </TableCell>
                  <TableCell>
                    <Chip size="small" label={item.status} />
                  </TableCell>
                  <TableCell>{new Date(item.createdAt).toLocaleString()}</TableCell>
                  <TableCell align="right">
                    <Button size="small" color="error" onClick={() => setDeleteId(item.id)}>
                      Delete
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {(query.data?.content.length ?? 0) === 0 && (
                <TableRow>
                  <TableCell colSpan={6}>No deployments found</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </QueryState>
      </Paper>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete deployment"
        description="This action cannot be undone."
        loading={removeMutation.isPending}
        onClose={() => setDeleteId(null)}
        onConfirm={() => deleteId && removeMutation.mutate(deleteId)}
      />
    </Stack>
  )
}

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
import { hostsApi } from '../../shared/api/endpoints'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog'
import { QueryState } from '../../shared/components/QueryState'

export function HostsListPage() {
  const [hostname, setHostname] = useState('')
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['hosts', hostname],
    queryFn: () => hostsApi.list({ hostname: hostname || undefined, page: 0, size: 50 }),
  })

  const removeMutation = useMutation({
    mutationFn: (id: string) => hostsApi.remove(id),
    onSuccess: async () => {
      setDeleteId(null)
      setDeleteError(null)
      await queryClient.invalidateQueries({ queryKey: ['hosts'] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
    },
    onError: () => setDeleteError('Unable to delete host. It may have deployments.'),
  })

  return (
    <Stack spacing={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" gap={2} flexWrap="wrap">
        <Box>
          <Typography variant="h4">Hosts</Typography>
          <Typography color="text.secondary">Register servers that will run applications.</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/hosts/new')}>
          New host
        </Button>
      </Box>

      <TextField
        label="Filter by hostname"
        value={hostname}
        onChange={(e) => setHostname(e.target.value)}
        sx={{ maxWidth: 360 }}
      />

      <Paper>
        <QueryState isLoading={query.isLoading} isError={query.isError}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Hostname</TableCell>
                <TableCell>IP</TableCell>
                <TableCell>OS</TableCell>
                <TableCell>Online</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(query.data?.content ?? []).map((host) => (
                <TableRow key={host.id} hover>
                  <TableCell>
                    <Link component={RouterLink} to={`/hosts/${host.id}`}>
                      {host.hostname}
                    </Link>
                  </TableCell>
                  <TableCell>{host.ip}</TableCell>
                  <TableCell>{host.operatingSystem}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      color={host.online ? 'success' : 'default'}
                      label={host.online ? 'Online' : 'Offline'}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Button size="small" onClick={() => navigate(`/hosts/${host.id}/edit`)}>
                      Edit
                    </Button>
                    <Button size="small" color="error" onClick={() => setDeleteId(host.id)}>
                      Delete
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {(query.data?.content.length ?? 0) === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>No hosts found</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </QueryState>
      </Paper>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete host"
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

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
import DnsOutlinedIcon from '@mui/icons-material/DnsOutlined'
import { hostsApi } from '../../shared/api/endpoints'
import { ConfirmDialog } from '../../shared/components/ConfirmDialog'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { StatusChip } from '../../shared/components/StatusChip'

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

  const rows = query.data?.content ?? []

  return (
    <PageShell>
      <PageHeader
        title="Hosts"
        description="Register servers that will run applications."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/hosts/new')}>
            New host
          </Button>
        }
      />

      <DataTableFrame
        toolbar={
          <TextField
            label="Filter by hostname"
            value={hostname}
            onChange={(e) => setHostname(e.target.value)}
            sx={{ maxWidth: 320, width: '100%' }}
          />
        }
      >
        <QueryState isLoading={query.isLoading} isError={query.isError}>
          {rows.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<DnsOutlinedIcon />}
                title={hostname ? 'No matches' : 'No hosts yet'}
                description={
                  hostname
                    ? 'Try a different hostname filter.'
                    : 'Add a LOCAL host for this Atlas server (or SSH) before Deploy. Then open the host and Sync.'
                }
                actionLabel={hostname ? undefined : 'New host'}
                onAction={hostname ? undefined : () => navigate('/hosts/new')}
              />
            </Box>
          ) : (
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
                {rows.map((host) => (
                  <TableRow key={host.id} hover>
                    <TableCell>
                      <Link
                        component={RouterLink}
                        to={`/hosts/${host.id}`}
                        underline="hover"
                        sx={{ fontWeight: 600 }}
                      >
                        {host.hostname}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" className="atlas-mono" color="text.secondary">
                        {host.ip}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {host.operatingSystem}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <StatusChip label={host.online ? 'ONLINE' : 'OFFLINE'} />
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
              </TableBody>
            </Table>
          )}
        </QueryState>
      </DataTableFrame>

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
    </PageShell>
  )
}

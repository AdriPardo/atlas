import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material'
import { applicationsApi, hostsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'

export function ApplicationDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [deployOpen, setDeployOpen] = useState(false)
  const [hostId, setHostId] = useState('')

  const query = useQuery({
    queryKey: ['applications', id],
    queryFn: () => applicationsApi.get(id),
    enabled: !!id,
  })

  const hostsQuery = useQuery({
    queryKey: ['hosts', 'deploy-picker'],
    queryFn: () => hostsApi.list({ size: 100, sort: 'hostname,asc' }),
    enabled: deployOpen,
  })

  const deployMutation = useMutation({
    mutationFn: () => applicationsApi.deploy(id, hostId),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: ['applications', id] })
      await queryClient.invalidateQueries({ queryKey: ['deployments'] })
      setDeployOpen(false)
      navigate(`/deployments/${result.deploymentId}`)
    },
  })

  return (
    <PageShell maxWidth={760}>
      <PageHeader
        title={query.data?.name ?? 'Application'}
        description="Definition and repository metadata."
        actions={
          <Stack direction="row" spacing={1}>
            <Button component={RouterLink} to="/applications">
              Back
            </Button>
            <Button variant="outlined" onClick={() => setDeployOpen(true)}>
              Deploy
            </Button>
            <Button variant="contained" onClick={() => navigate(`/applications/${id}/edit`)}>
              Edit
            </Button>
          </Stack>
        }
      />

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <DetailPanel>
            <DetailField label="Status">
              <StatusChip label={query.data.status} />
            </DetailField>
            <DetailField label="Description">{query.data.description || 'No description'}</DetailField>
            <DetailField label="Repository" mono>
              {query.data.repositoryUrl}
            </DetailField>
            <DetailField label="Branch" mono>
              {query.data.branch}
            </DetailField>
            <DetailField label="Compose path" mono>
              {query.data.composePath}
            </DetailField>
            <DetailField label="Domain">{query.data.domain || '-'}</DetailField>
            <DetailField label="Created">{new Date(query.data.createdAt).toLocaleString()}</DetailField>
            <DetailField label="Updated">{new Date(query.data.updatedAt).toLocaleString()}</DetailField>
          </DetailPanel>
        )}
      </QueryState>

      <Dialog open={deployOpen} onClose={() => setDeployOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Deploy application</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {deployMutation.isError && (
              <Alert severity="error" variant="outlined">
                Deploy request failed
              </Alert>
            )}
            <TextField
              select
              label="Target host"
              value={hostId}
              onChange={(e) => setHostId(e.target.value)}
              fullWidth
            >
              {(hostsQuery.data?.content ?? []).map((host) => (
                <MenuItem key={host.id} value={host.id}>
                  {host.hostname} ({host.connectionType}) — {host.ip}
                </MenuItem>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeployOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!hostId || deployMutation.isPending}
            onClick={() => deployMutation.mutate()}
          >
            Deploy
          </Button>
        </DialogActions>
      </Dialog>
    </PageShell>
  )
}

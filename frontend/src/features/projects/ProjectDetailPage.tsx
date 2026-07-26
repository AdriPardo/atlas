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
  Typography,
} from '@mui/material'
import { hostsApi, projectsApi, servicesApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'

export function ProjectDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [deployOpen, setDeployOpen] = useState(false)
  const [hostId, setHostId] = useState('')
  const [serviceId, setServiceId] = useState('')

  const query = useQuery({
    queryKey: ['projects', id],
    queryFn: () => projectsApi.get(id),
    enabled: !!id,
  })

  const servicesQuery = useQuery({
    queryKey: ['projects', id, 'services'],
    queryFn: () => projectsApi.listServices(id, { size: 50 }),
    enabled: !!id,
  })

  const hostsQuery = useQuery({
    queryKey: ['hosts', 'deploy-picker'],
    queryFn: () => hostsApi.list({ size: 100, sort: 'hostname,asc' }),
    enabled: deployOpen,
  })

  const defaultService = servicesQuery.data?.content?.[0]
  const deployTargetId = serviceId || defaultService?.id || ''

  const deployMutation = useMutation({
    mutationFn: () =>
      deployTargetId
        ? servicesApi.deploy(deployTargetId, hostId)
        : projectsApi.deploy(id, hostId),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: ['projects', id] })
      await queryClient.invalidateQueries({ queryKey: ['deployments'] })
      setDeployOpen(false)
      navigate(`/deployments/${result.deploymentId}`)
    },
  })

  return (
    <PageShell maxWidth={760}>
      <PageHeader
        title={query.data?.name ?? 'Project'}
        description="Project and its deployable services."
        actions={
          <Stack direction="row" spacing={1}>
            <Button component={RouterLink} to="/projects">
              Back
            </Button>
            <Button variant="outlined" onClick={() => setDeployOpen(true)}>
              Deploy
            </Button>
            <Button variant="contained" onClick={() => navigate(`/projects/${id}/edit`)}>
              Edit
            </Button>
          </Stack>
        }
      />

      <QueryState isLoading={query.isLoading || servicesQuery.isLoading} isError={query.isError}>
        {query.data && (
          <Stack spacing={3}>
            <DetailPanel>
              <DetailField label="Status">
                <StatusChip label={query.data.status} />
              </DetailField>
              <DetailField label="Slug" mono>
                {query.data.slug}
              </DetailField>
              <DetailField label="Description">{query.data.description || 'No description'}</DetailField>
              <DetailField label="Created">{new Date(query.data.createdAt).toLocaleString()}</DetailField>
              <DetailField label="Updated">{new Date(query.data.updatedAt).toLocaleString()}</DetailField>
            </DetailPanel>

            <DetailPanel>
              <Typography variant="subtitle1" sx={{ mb: 1 }}>
                Services
              </Typography>
              {(servicesQuery.data?.content ?? []).map((svc) => (
                <Stack key={svc.id} spacing={1} sx={{ mb: 2 }}>
                  <DetailField label="Name">
                    {svc.name} <StatusChip label={svc.status} />
                  </DetailField>
                  <DetailField label="Repository" mono>
                    {svc.repositoryUrl}
                  </DetailField>
                  <DetailField label="Branch" mono>
                    {svc.branch}
                  </DetailField>
                  <DetailField label="Compose path" mono>
                    {svc.composePath}
                  </DetailField>
                  <DetailField label="Domain">{svc.domain || '-'}</DetailField>
                </Stack>
              ))}
              {(servicesQuery.data?.content.length ?? 0) === 0 && (
                <Typography variant="body2" color="text.secondary">
                  No services yet.
                </Typography>
              )}
            </DetailPanel>
          </Stack>
        )}
      </QueryState>

      <Dialog open={deployOpen} onClose={() => setDeployOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Deploy service</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {deployMutation.isError && (
              <Alert severity="error" variant="outlined">
                Deploy request failed
              </Alert>
            )}
            <TextField
              select
              label="Service"
              value={serviceId || defaultService?.id || ''}
              onChange={(e) => setServiceId(e.target.value)}
              fullWidth
            >
              {(servicesQuery.data?.content ?? []).map((svc) => (
                <MenuItem key={svc.id} value={svc.id}>
                  {svc.name}
                </MenuItem>
              ))}
            </TextField>
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
            disabled={!hostId || !deployTargetId || deployMutation.isPending}
            onClick={() => deployMutation.mutate()}
          >
            Deploy
          </Button>
        </DialogActions>
      </Dialog>
    </PageShell>
  )
}

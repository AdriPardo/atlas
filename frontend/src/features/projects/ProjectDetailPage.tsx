import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import { hostsApi, projectsApi, servicesApi } from '../../shared/api/endpoints'
import type { PlacementMode, ServiceExposure } from '../../shared/types/api'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'
import { ProjectMembersPanel } from './ProjectMembersPanel'
import { ProjectDomainsPanel } from './ProjectDomainsPanel'
import { ProjectSecretsPanel } from './ProjectSecretsPanel'

export function ProjectDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [deployOpen, setDeployOpen] = useState(false)
  const [hostId, setHostId] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [exposure, setExposure] = useState<ServiceExposure>('PUBLIC')
  const [placementMode, setPlacementMode] = useState<PlacementMode>('SHARED')

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
  const hosts = hostsQuery.data?.content ?? []

  const deployMutation = useMutation({
    mutationFn: () =>
      deployTargetId
        ? servicesApi.deploy(deployTargetId, {
            hostId: hostId || undefined,
            exposure,
            placementMode,
          })
        : projectsApi.deploy(id, { hostId: hostId || undefined, exposure, placementMode }),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: ['projects', id] })
      await queryClient.invalidateQueries({ queryKey: ['deployments'] })
      await queryClient.invalidateQueries({ queryKey: ['projects', id, 'services'] })
      setDeployOpen(false)
      navigate(`/deployments/${result.deploymentId}`)
    },
  })

  return (
    <PageShell maxWidth={760}>
      <PageHeader
        title={query.data?.name ?? 'Project'}
        description="Connect the app; Atlas places and deploys it."
        actions={
          <Stack direction="row" spacing={1}>
            <Button component={RouterLink} to="/projects">
              Back
            </Button>
            <Button variant="contained" onClick={() => setDeployOpen(true)}>
              Deploy
            </Button>
            <Button variant="outlined" onClick={() => navigate(`/projects/${id}/edit`)}>
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
                  <DetailField label="Exposure">
                    <StatusChip label={svc.exposure ?? 'PUBLIC'} />
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

            <ProjectSecretsPanel projectId={id} />

            <ProjectDomainsPanel
              projectId={id}
              services={servicesQuery.data?.content ?? []}
            />

            <ProjectMembersPanel projectId={id} />
          </Stack>
        )}
      </QueryState>

      <Dialog open={deployOpen} onClose={() => setDeployOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Deploy</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {deployMutation.isError && (
              <Alert severity="error" variant="outlined">
                Deploy request failed
              </Alert>
            )}
            <Alert severity="info" variant="outlined">
              Atlas picks where to run (shared LOCAL by default). Isolated requests a Proxmox VM when
              configured; otherwise falls back to shared. Private GitHub repos need{' '}
              <strong>git.token</strong> on this project (owned or linked) or as an organization secret.
            </Alert>
            <Typography variant="body2" color="text.secondary">
              Exposure
            </Typography>
            <ToggleButtonGroup
              exclusive
              fullWidth
              size="small"
              value={exposure}
              onChange={(_, value: ServiceExposure | null) => {
                if (value) setExposure(value)
              }}
            >
              <ToggleButton value="PUBLIC">Public</ToggleButton>
              <ToggleButton value="INTERNAL">Internal</ToggleButton>
            </ToggleButtonGroup>
            <Typography variant="caption" color="text.secondary">
              Public creates a domain stub + Traefik metadata. Internal stays LAN / private entrypoint
              only.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Placement
            </Typography>
            <ToggleButtonGroup
              exclusive
              fullWidth
              size="small"
              value={placementMode}
              onChange={(_, value: PlacementMode | null) => {
                if (value) setPlacementMode(value)
              }}
            >
              <ToggleButton value="SHARED">Shared host</ToggleButton>
              <ToggleButton value="ISOLATED">Isolated VM</ToggleButton>
            </ToggleButtonGroup>
            <Typography variant="caption" color="text.secondary">
              Shared reuses the local Docker host. Isolated asks Proxmox for a dedicated VM (falls back
              to shared until clone + guest IP are ready).
            </Typography>
            {(servicesQuery.data?.content?.length ?? 0) > 1 && (
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
            )}
            <Accordion disableGutters elevation={0} sx={{ border: '1px solid', borderColor: 'divider' }}>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography variant="body2">Advanced — host override</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Stack spacing={1.5}>
                  <Typography variant="caption" color="text.secondary">
                    Leave empty for Autopilot placement. Manual Host management lives under Hosts.
                  </Typography>
                  <TextField
                    select
                    label="Target host (optional)"
                    value={hostId}
                    onChange={(e) => setHostId(e.target.value)}
                    fullWidth
                    helperText="Optional. Atlas will seed atlas-local if no hosts exist."
                  >
                    <MenuItem value="">
                      <em>Autopilot (recommended)</em>
                    </MenuItem>
                    {hosts.map((host) => (
                      <MenuItem key={host.id} value={host.id}>
                        {host.hostname} ({host.connectionType}) — {host.ip}
                        {host.online ? '' : ' [offline]'}
                      </MenuItem>
                    ))}
                  </TextField>
                  <Button size="small" component={RouterLink} to="/hosts" sx={{ alignSelf: 'flex-start' }}>
                    Open Hosts (advanced)
                  </Button>
                </Stack>
              </AccordionDetails>
            </Accordion>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeployOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!deployTargetId || deployMutation.isPending}
            onClick={() => deployMutation.mutate()}
          >
            Deploy
          </Button>
        </DialogActions>
      </Dialog>
    </PageShell>
  )
}

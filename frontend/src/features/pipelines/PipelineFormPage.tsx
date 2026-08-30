import { useEffect, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { hostsApi, pipelinesApi, projectsApi, servicesApi } from '../../shared/api/endpoints'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'

import { useAuthQuery } from '../auth/useAuthQuery'
export function PipelineFormPage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [projectId, setProjectId] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [hostId, setHostId] = useState('')
  const [error, setError] = useState<string | null>(null)

  const projectsQuery = useAuthQuery({
    queryKey: ['projects', 'pipeline-form'],
    queryFn: () => projectsApi.list({ page: 0, size: 100 }),
  })
  const servicesQuery = useAuthQuery({
    queryKey: ['services', 'pipeline-form', projectId],
    queryFn: () =>
      projectId
        ? projectsApi.listServices(projectId, { page: 0, size: 100 })
        : servicesApi.list({ page: 0, size: 100 }),
    enabled: !!projectId,
  })
  const hostsQuery = useAuthQuery({
    queryKey: ['hosts', 'pipeline-form'],
    queryFn: () => hostsApi.list({ page: 0, size: 100 }),
  })

  useEffect(() => {
    setServiceId('')
  }, [projectId])

  const createMutation = useMutation({
    mutationFn: () =>
      pipelinesApi.create({
        projectId,
        name,
        serviceId,
        hostId: hostId || undefined,
      }),
    onSuccess: (pipeline) => navigate(`/pipelines/${pipeline.id}`),
    onError: () => setError('Unable to create pipeline. Check name uniqueness and selections.'),
  })

  const projects = projectsQuery.data?.content ?? []
  const services = servicesQuery.data?.content ?? []
  const hosts = hostsQuery.data?.content ?? []
  const canSubmit = name.trim() && projectId && serviceId && !createMutation.isPending

  return (
    <PageShell maxWidth={640}>
      <PageHeader
        title="New pipeline"
        description="Bind a service deploy. Leave host empty for Autopilot placement on each run."
        actions={
          <Button onClick={() => navigate('/pipelines')}>Cancel</Button>
        }
      />

      <Stack spacing={2} component="form" onSubmit={(e) => {
        e.preventDefault()
        if (canSubmit) createMutation.mutate()
      }}>
        {error && (
          <Alert severity="error" variant="outlined">
            {error}
          </Alert>
        )}
        <TextField
          label="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          fullWidth
        />
        <FormControl fullWidth required>
          <InputLabel id="pipeline-project">Project</InputLabel>
          <Select
            labelId="pipeline-project"
            label="Project"
            value={projectId}
            onChange={(e) => setProjectId(e.target.value)}
          >
            {projects.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                {p.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl fullWidth required disabled={!projectId}>
          <InputLabel id="pipeline-service">Service</InputLabel>
          <Select
            labelId="pipeline-service"
            label="Service"
            value={serviceId}
            onChange={(e) => setServiceId(e.target.value)}
          >
            {services.map((s) => (
              <MenuItem key={s.id} value={s.id}>
                {s.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Accordion disableGutters elevation={0} sx={{ border: '1px solid', borderColor: 'divider' }}>
          <AccordionSummary expandIcon={<ExpandMoreIcon />}>
            <Typography variant="body2">Advanced — host override</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Stack spacing={1.5}>
              <Typography variant="caption" color="text.secondary">
                Leave empty for Autopilot placement (SHARED) on each webhook/run.
              </Typography>
              <TextField
                select
                label="Target host (optional)"
                value={hostId}
                onChange={(e) => setHostId(e.target.value)}
                fullWidth
                helperText="Optional pin. Empty = resolve host per run like manual deploy."
              >
                <MenuItem value="">
                  <em>Autopilot (recommended)</em>
                </MenuItem>
                {hosts.map((h) => (
                  <MenuItem key={h.id} value={h.id}>
                    {h.hostname} ({h.ip})
                  </MenuItem>
                ))}
              </TextField>
            </Stack>
          </AccordionDetails>
        </Accordion>
        <Button type="submit" variant="contained" disabled={!canSubmit}>
          Create pipeline
        </Button>
      </Stack>
    </PageShell>
  )
}

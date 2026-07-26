import { useEffect, useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  Alert,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
} from '@mui/material'
import { hostsApi, pipelinesApi, projectsApi, servicesApi } from '../../shared/api/endpoints'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'

export function PipelineFormPage() {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [projectId, setProjectId] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [hostId, setHostId] = useState('')
  const [error, setError] = useState<string | null>(null)

  const projectsQuery = useQuery({
    queryKey: ['projects', 'pipeline-form'],
    queryFn: () => projectsApi.list({ page: 0, size: 100 }),
  })
  const servicesQuery = useQuery({
    queryKey: ['services', 'pipeline-form', projectId],
    queryFn: () =>
      projectId
        ? projectsApi.listServices(projectId, { page: 0, size: 100 })
        : servicesApi.list({ page: 0, size: 100 }),
    enabled: !!projectId,
  })
  const hostsQuery = useQuery({
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
        hostId,
      }),
    onSuccess: (pipeline) => navigate(`/pipelines/${pipeline.id}`),
    onError: () => setError('Unable to create pipeline. Check name uniqueness and selections.'),
  })

  const projects = projectsQuery.data?.content ?? []
  const services = servicesQuery.data?.content ?? []
  const hosts = hostsQuery.data?.content ?? []
  const canSubmit = name.trim() && projectId && serviceId && hostId && !createMutation.isPending

  return (
    <PageShell maxWidth={640}>
      <PageHeader
        title="New pipeline"
        description="Bind a service deploy to a host. Running the pipeline enqueues DEPLOY_SERVICE."
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
        <FormControl fullWidth required>
          <InputLabel id="pipeline-host">Host</InputLabel>
          <Select
            labelId="pipeline-host"
            label="Host"
            value={hostId}
            onChange={(e) => setHostId(e.target.value)}
          >
            {hosts.map((h) => (
              <MenuItem key={h.id} value={h.id}>
                {h.hostname} ({h.ip})
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Button type="submit" variant="contained" disabled={!canSubmit}>
          Create pipeline
        </Button>
      </Stack>
    </PageShell>
  )
}

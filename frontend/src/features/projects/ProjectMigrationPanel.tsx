import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
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
import { servicesApi } from '../../shared/api/endpoints'
import { getApiErrorMessage } from '../../shared/api/queryErrors'
import type { Service } from '../../shared/types/api'
import { DetailPanel } from '../../shared/components/DetailPanel'

import { useAuthQuery } from '../auth/useAuthQuery'

const REELPATH_DEFAULT_COMMAND = 'npm run migrate:deploy -w @autotube/database'

type MigrationStrategy = 'prisma' | 'flyway' | 'custom' | ''

export function ProjectMigrationPanel({
  projectId,
  serviceId,
}: {
  projectId: string
  serviceId?: string
}) {
  const queryClient = useQueryClient()
  const [enabled, setEnabled] = useState<boolean | null>(null)
  const [strategy, setStrategy] = useState<MigrationStrategy>('')
  const [command, setCommand] = useState('')
  const [container, setContainer] = useState('api')

  const serviceQuery = useAuthQuery({
    queryKey: ['services', serviceId],
    queryFn: () => servicesApi.get(serviceId!),
    enabled: !!serviceId,
  })

  const service = serviceQuery.data

  useEffect(() => {
    if (!service) return
    setEnabled(service.migrationEnabled ?? null)
    setStrategy((service.migrationStrategy as MigrationStrategy) ?? '')
    setCommand(service.migrationCommand ?? '')
    setContainer(service.migrationContainer ?? 'api')
  }, [service])

  const saveMutation = useMutation({
    mutationFn: (body: Parameters<typeof servicesApi.update>[1]) =>
      servicesApi.update(serviceId!, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['services', serviceId] })
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'services'] })
    },
  })

  if (!serviceId) {
    return (
      <Typography variant="body2" color="text.secondary">
        No default service — create a service first.
      </Typography>
    )
  }

  if (serviceQuery.isLoading) {
    return <Typography variant="body2">Loading service…</Typography>
  }

  if (serviceQuery.isError || !service) {
    return (
      <Alert severity="error" variant="outlined">
        {getApiErrorMessage(serviceQuery.error)}
      </Alert>
    )
  }

  const inheritLabel =
    enabled === null
      ? 'Inherit from atlas.yml (runtime.migration / migrateCommand)'
      : enabled
        ? 'Run migration after each deploy'
        : 'Disabled (skip even if atlas.yml declares migration)'

  const onSave = () => {
    saveMutation.mutate(buildUpdatePayload(service, enabled, strategy, command, container))
  }

  const applyReelpathPreset = () => {
    setEnabled(true)
    setStrategy('prisma')
    setCommand(REELPATH_DEFAULT_COMMAND)
    setContainer('api')
  }

  return (
    <DetailPanel title="Post-deploy migrations">
      <Stack spacing={2}>
        <Alert severity="info" variant="outlined">
          Atlas runs migrate <strong>after</strong> compose up. With a container set, the command runs
          via <code>docker compose exec -T</code>. Disable here if the app entrypoint already migrates.
        </Alert>

        <FormControl fullWidth size="small">
          <InputLabel id="migration-enabled-label">Migration mode</InputLabel>
          <Select
            labelId="migration-enabled-label"
            label="Migration mode"
            value={enabled === null ? 'inherit' : enabled ? 'on' : 'off'}
            onChange={(e) => {
              const v = e.target.value
              setEnabled(v === 'inherit' ? null : v === 'on')
            }}
          >
            <MenuItem value="inherit">Inherit from repo manifest</MenuItem>
            <MenuItem value="on">Enabled (platform override)</MenuItem>
            <MenuItem value="off">Disabled</MenuItem>
          </Select>
        </FormControl>

        <Typography variant="caption" color="text.secondary">
          {inheritLabel}
        </Typography>

        {(enabled === true || enabled === null) && (
          <>
            <FormControl fullWidth size="small">
              <InputLabel id="migration-strategy-label">Strategy</InputLabel>
              <Select
                labelId="migration-strategy-label"
                label="Strategy"
                value={strategy || 'custom'}
                onChange={(e) => setStrategy(e.target.value as MigrationStrategy)}
              >
                <MenuItem value="prisma">Prisma (migrate deploy)</MenuItem>
                <MenuItem value="flyway">Flyway</MenuItem>
                <MenuItem value="custom">Custom command</MenuItem>
              </Select>
            </FormControl>

            <TextField
              label="Compose service (container)"
              size="small"
              fullWidth
              value={container}
              onChange={(e) => setContainer(e.target.value)}
              helperText="Default api — used for docker compose exec -T"
            />

            <TextField
              label="Migration command (inner)"
              size="small"
              fullWidth
              multiline
              minRows={2}
              value={command}
              onChange={(e) => setCommand(e.target.value)}
              placeholder={REELPATH_DEFAULT_COMMAND}
              helperText="Required for custom; optional override for prisma/flyway defaults"
            />

            <Button size="small" variant="outlined" onClick={applyReelpathPreset}>
              Apply Reelpath / Autotube preset
            </Button>
          </>
        )}

        {saveMutation.isError && (
          <Alert severity="error" variant="outlined">
            {getApiErrorMessage(saveMutation.error)}
          </Alert>
        )}

        <Stack direction="row" spacing={1}>
          <Button variant="contained" onClick={onSave} disabled={saveMutation.isPending}>
            Save migration settings
          </Button>
        </Stack>
      </Stack>
    </DetailPanel>
  )
}

function buildUpdatePayload(
  service: Service,
  enabled: boolean | null,
  strategy: MigrationStrategy,
  command: string,
  container: string,
): Parameters<typeof servicesApi.update>[1] {
  return {
    name: service.name,
    repositoryUrl: service.repositoryUrl,
    branch: service.branch,
    composePath: service.composePath ?? '',
    domain: service.domain,
    environment: service.environment,
    status: service.status,
    migrationEnabled: enabled,
    migrationStrategy: strategy || undefined,
    migrationCommand: command.trim() || undefined,
    migrationContainer: container.trim() || undefined,
  }
}

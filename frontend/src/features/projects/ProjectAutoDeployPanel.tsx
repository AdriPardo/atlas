import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import {
  Alert,
  Button,
  IconButton,
  Link,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { pipelinesApi } from '../../shared/api/endpoints'
import type { AutoDeployResult, Service } from '../../shared/types/api'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'

function webhookUrlFor(token: string) {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return `${origin}/api/v1/webhooks/git/${token}`
}

type Props = {
  projectId: string
  services: Service[]
}

export function ProjectAutoDeployPanel({ projectId, services }: Props) {
  const queryClient = useQueryClient()
  const [copied, setCopied] = useState(false)
  const [lastResult, setLastResult] = useState<AutoDeployResult | null>(null)
  const [activeServiceId, setActiveServiceId] = useState<string | null>(null)

  const pipelinesQuery = useQuery({
    queryKey: ['pipelines', 'project', projectId],
    queryFn: () => pipelinesApi.list({ projectId, page: 0, size: 50 }),
    enabled: !!projectId,
  })

  const enableMutation = useMutation({
    mutationFn: (serviceId: string) =>
      pipelinesApi.enableAutoDeploy({
        serviceId,
        publicBaseUrl: typeof window !== 'undefined' ? window.location.origin : undefined,
      }),
    onSuccess: async (result, serviceId) => {
      setLastResult(result)
      setActiveServiceId(serviceId)
      await queryClient.invalidateQueries({ queryKey: ['pipelines'] })
    },
  })

  const pipelinesByService = useMemo(() => {
    const map = new Map<string, string>()
    for (const p of pipelinesQuery.data?.content ?? []) {
      if (!map.has(p.serviceId)) {
        map.set(p.serviceId, p.id)
      }
    }
    return map
  }, [pipelinesQuery.data])

  const deployable = services.filter((s) => !!s.repositoryUrl)
  if (deployable.length === 0) {
    return null
  }

  const copy = async (value: string) => {
    await navigator.clipboard.writeText(value)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 1800)
  }

  const displayUrl =
    lastResult?.webhookUrl ||
    (lastResult?.pipeline.webhookToken ? webhookUrlFor(lastResult.pipeline.webhookToken) : '')

  return (
    <DetailPanel title="Auto-deploy on push">
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
        Push to the service branch enqueues a deploy. Atlas creates a pipeline and, when{' '}
        <strong>git.token</strong> is set, tries to register the GitHub webhook for you.
      </Typography>

      {enableMutation.isError && (
        <Alert severity="error" variant="outlined" sx={{ mb: 1.5 }}>
          Could not enable auto-deploy.
        </Alert>
      )}

      {lastResult && (
        <Alert
          severity={lastResult.githubWebhookRegistered ? 'success' : 'info'}
          variant="outlined"
          sx={{ mb: 1.5 }}
        >
          {lastResult.githubWebhookRegistered
            ? `GitHub webhook registered. Pushes to ${lastResult.trackedBranch} will deploy.`
            : 'Pipeline ready. Finish GitHub webhook setup with the URL below (git.token missing or API skipped).'}
        </Alert>
      )}

      <Stack spacing={2}>
        {deployable.map((svc) => {
          const pipelineId = pipelinesByService.get(svc.id)
          const enabled = !!pipelineId
          return (
            <Stack
              key={svc.id}
              spacing={1}
              sx={{ borderTop: '1px solid', borderColor: 'divider', pt: 1.5 }}
            >
              <DetailField label="Service">
                {svc.name} · branch <span className="atlas-mono">{svc.branch}</span>
              </DetailField>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <Button
                  variant={enabled ? 'outlined' : 'contained'}
                  disabled={enableMutation.isPending && activeServiceId === svc.id}
                  onClick={() => {
                    setActiveServiceId(svc.id)
                    enableMutation.mutate(svc.id)
                  }}
                >
                  {enabled ? 'Refresh auto-deploy' : 'Enable auto-deploy'}
                </Button>
                {pipelineId && (
                  <Button component={RouterLink} to={`/pipelines/${pipelineId}`}>
                    Open pipeline
                  </Button>
                )}
              </Stack>
            </Stack>
          )
        })}
      </Stack>

      {lastResult && (
        <Stack spacing={1.5} sx={{ mt: 2 }}>
          <DetailField label="Webhook URL">
            <Stack direction="row" spacing={1} alignItems="center">
              <TextField
                size="small"
                fullWidth
                value={displayUrl}
                InputProps={{ readOnly: true, className: 'atlas-mono' }}
              />
              <Tooltip title={copied ? 'Copied' : 'Copy URL'}>
                <IconButton onClick={() => void copy(displayUrl)} aria-label="Copy webhook URL">
                  <ContentCopyIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Stack>
          </DetailField>
          <DetailField label="Secret">
            <Stack direction="row" spacing={1} alignItems="center">
              <TextField
                size="small"
                fullWidth
                value={lastResult.pipeline.webhookToken}
                InputProps={{ readOnly: true, className: 'atlas-mono' }}
              />
              <Tooltip title="Copy secret">
                <IconButton
                  onClick={() => void copy(lastResult.pipeline.webhookToken)}
                  aria-label="Copy webhook secret"
                >
                  <ContentCopyIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Stack>
          </DetailField>
          {!lastResult.githubWebhookRegistered && (
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{ whiteSpace: 'pre-wrap' }}
              className="atlas-mono"
            >
              {lastResult.setupInstructions}
            </Typography>
          )}
          <Typography variant="caption" color="text.secondary">
            Pipeline{' '}
            <Link component={RouterLink} to={`/pipelines/${lastResult.pipeline.id}`}>
              {lastResult.pipeline.name}
            </Link>
            {' · '}only <strong>push</strong> events to branch{' '}
            <span className="atlas-mono">{lastResult.trackedBranch}</span> trigger deploys.
          </Typography>
        </Stack>
      )}
    </DetailPanel>
  )
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  IconButton,
  Link,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'
import { pipelinesApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { useMemo, useState } from 'react'

function webhookUrlFor(token: string) {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return `${origin}/api/v1/webhooks/git/${token}`
}

export function PipelineDetailPage() {
  const { id = '' } = useParams()
  const queryClient = useQueryClient()
  const [copied, setCopied] = useState(false)

  const query = useQuery({
    queryKey: ['pipelines', id],
    queryFn: () => pipelinesApi.get(id),
    enabled: !!id,
  })

  const runsQuery = useQuery({
    queryKey: ['pipelines', id, 'runs'],
    queryFn: () => pipelinesApi.listRuns(id, { page: 0, size: 50 }),
    enabled: !!id,
    refetchInterval: (q) => {
      const rows = q.state.data?.content ?? []
      return rows.some((r) => r.status === 'PENDING' || r.status === 'RUNNING') ? 2500 : false
    },
  })

  const runMutation = useMutation({
    mutationFn: () => pipelinesApi.run(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['pipelines', id, 'runs'] })
      await queryClient.invalidateQueries({ queryKey: ['deployments'] })
      await queryClient.invalidateQueries({ queryKey: ['jobs'] })
    },
  })

  const rotateMutation = useMutation({
    mutationFn: () => pipelinesApi.rotateWebhookToken(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['pipelines', id] })
    },
  })

  const runs = runsQuery.data?.content ?? []
  const webhookUrl = useMemo(
    () => (query.data?.webhookToken ? webhookUrlFor(query.data.webhookToken) : ''),
    [query.data?.webhookToken],
  )

  const copyUrl = async () => {
    if (!webhookUrl) return
    await navigator.clipboard.writeText(webhookUrl)
    setCopied(true)
    window.setTimeout(() => setCopied(false), 1800)
  }

  return (
    <PageShell maxWidth={960}>
      <PageHeader
        title={query.data?.name ?? 'Pipeline'}
        description="Deploy pipeline — Run now or trigger via git webhook."
        actions={
          <Stack direction="row" spacing={1}>
            <Button component={RouterLink} to="/pipelines">
              Back
            </Button>
            <Button
              variant="contained"
              onClick={() => runMutation.mutate()}
              disabled={runMutation.isPending || !query.data}
            >
              Run now
            </Button>
          </Stack>
        }
      />

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <Stack spacing={2.5}>
            {runMutation.isSuccess && (
              <Alert severity="success" variant="outlined">
                Pipeline run accepted. Deployment{' '}
                <Link component={RouterLink} to={`/deployments/${runMutation.data.deploymentId}`}>
                  {runMutation.data.deploymentId?.slice(0, 8)}
                </Link>{' '}
                queued.
              </Alert>
            )}
            {runMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to run pipeline.
              </Alert>
            )}
            {rotateMutation.isSuccess && (
              <Alert severity="info" variant="outlined">
                Webhook token rotated. Update the secret in GitHub/Gitea to the new token.
              </Alert>
            )}
            {rotateMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to rotate webhook token.
              </Alert>
            )}

            <DetailPanel>
              <DetailField label="Project" mono>
                <Link component={RouterLink} to={`/projects/${query.data.projectId}`}>
                  {query.data.projectId.slice(0, 8)}
                </Link>
              </DetailField>
              <DetailField label="Service" mono>
                {query.data.serviceId}
              </DetailField>
              <DetailField label="Host" mono>
                <Link component={RouterLink} to={`/hosts/${query.data.hostId}`}>
                  {query.data.hostId.slice(0, 8)}
                </Link>
              </DetailField>
              <DetailField label="Created">
                {new Date(query.data.createdAt).toLocaleString()}
              </DetailField>
            </DetailPanel>

            <DetailPanel title="Git webhook (auto-deploy)">
              <Alert severity="info" variant="outlined" sx={{ mb: 1 }}>
                GitHub → Settings → Webhooks → Add webhook. Events: <strong>Just the push event</strong>.
                Atlas ignores ping/PR and only deploys pushes that match the service branch.
              </Alert>
              <DetailField label="Payload URL">
                <Stack direction="row" spacing={1} alignItems="center">
                  <TextField
                    size="small"
                    fullWidth
                    value={webhookUrl}
                    InputProps={{ readOnly: true, className: 'atlas-mono' }}
                  />
                  <Tooltip title={copied ? 'Copied' : 'Copy URL'}>
                    <IconButton onClick={() => void copyUrl()} aria-label="Copy webhook URL">
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
                    value={query.data.webhookToken}
                    InputProps={{ readOnly: true, className: 'atlas-mono' }}
                  />
                  <Tooltip title="Copy secret">
                    <IconButton
                      onClick={() => void navigator.clipboard.writeText(query.data.webhookToken)}
                      aria-label="Copy webhook secret"
                    >
                      <ContentCopyIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </Stack>
              </DetailField>
              <DetailField label="Content type">application/json</DetailField>
              <DetailField label="Tip">
                Prefer <strong>Enable auto-deploy</strong> on the Project page — it creates this
                pipeline and registers the GitHub webhook when <code>git.token</code> is present.
              </DetailField>
              <DetailField label="Actions">
                <Button
                  variant="outlined"
                  color="warning"
                  onClick={() => rotateMutation.mutate()}
                  disabled={rotateMutation.isPending}
                >
                  Rotate token
                </Button>
              </DetailField>
            </DetailPanel>

            <Stack spacing={1}>
              <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
                Runs
              </Typography>
              <QueryState isLoading={runsQuery.isLoading} isError={runsQuery.isError}>
                {runs.length === 0 ? (
                  <EmptyState
                    title="No runs yet"
                    description="Click Run now or POST to the git webhook to enqueue a deploy."
                    actionLabel="Run now"
                    onAction={() => runMutation.mutate()}
                  />
                ) : (
                  <DataTableFrame>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Status</TableCell>
                          <TableCell>Triggered</TableCell>
                          <TableCell>Deployment</TableCell>
                          <TableCell>Job</TableCell>
                          <TableCell>Started</TableCell>
                          <TableCell>Error</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {runs.map((run) => (
                          <TableRow key={run.id} hover>
                            <TableCell>
                              <StatusChip label={run.status} />
                            </TableCell>
                            <TableCell>{run.triggeredBy}</TableCell>
                            <TableCell>
                              {run.deploymentId ? (
                                <Link
                                  component={RouterLink}
                                  to={`/deployments/${run.deploymentId}`}
                                  className="atlas-mono"
                                  underline="hover"
                                >
                                  {run.deploymentId.slice(0, 8)}
                                </Link>
                              ) : (
                                '-'
                              )}
                            </TableCell>
                            <TableCell>
                              <Typography variant="body2" className="atlas-mono">
                                {run.jobId ? run.jobId.slice(0, 8) : '-'}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              {run.startedAt ? new Date(run.startedAt).toLocaleString() : '-'}
                            </TableCell>
                            <TableCell>
                              <Typography variant="caption" color="error">
                                {run.lastError || ''}
                              </Typography>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </DataTableFrame>
                )}
              </QueryState>
            </Stack>
          </Stack>
        )}
      </QueryState>
    </PageShell>
  )
}

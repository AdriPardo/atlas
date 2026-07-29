import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { Alert, Box, Button, LinearProgress, Link, Stack, Typography } from '@mui/material'
import { deploymentsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'
import { LogViewer } from '../../shared/components/LogViewer'

const ACTIVE = new Set(['PENDING', 'RUNNING'])

function progressHint(status: string): string {
  if (status === 'PENDING') return 'Queued — waiting for a worker'
  if (status === 'RUNNING') return 'Deploy in progress'
  if (status === 'SUCCEEDED') return 'Finished successfully'
  if (status === 'FAILED') return 'Finished with errors'
  if (status === 'CANCELLED') return 'Cancelled'
  return status
}

export function DeploymentDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: ['deployments', id],
    queryFn: () => deploymentsApi.get(id),
    enabled: !!id,
    refetchInterval: (q) => {
      const status = q.state.data?.status
      return status && ACTIVE.has(status) ? 2500 : false
    },
  })

  const live = query.data ? ACTIVE.has(query.data.status) : false

  return (
    <PageShell>
      <PageHeader
        title="Deployment"
        description={query.data ? `Run ${query.data.id.slice(0, 8)}` : 'Deployment record'}
        actions={
          <Button component={RouterLink} to="/deployments">
            Back
          </Button>
        }
      />

      <QueryState
        isLoading={query.isLoading}
        isError={query.isError}
        onRetry={() => query.refetch()}
        skeleton="detail"
        errorMessage="Could not load this deployment."
      >
        {query.data && (
          <Stack spacing={3}>
            {live && (
              <Box>
                <Box display="flex" justifyContent="space-between" alignItems="center" mb={1} gap={1}>
                  <Typography variant="body2" color="text.secondary">
                    {progressHint(query.data.status)}
                  </Typography>
                  <StatusChip label={query.data.status} />
                </Box>
                <LinearProgress
                  color="primary"
                  sx={{ height: 3, borderRadius: 1, bgcolor: (t) => (t.palette.mode === 'dark' ? 'rgba(45,212,191,0.12)' : 'rgba(15,118,110,0.1)') }}
                />
              </Box>
            )}

            {query.data.status === 'FAILED' && (
              <Alert severity="error" variant="outlined">
                Deploy failed. Review the logs below for compose or host errors.
              </Alert>
            )}

            <DetailPanel title="Summary">
              <DetailField label="Status">
                <Stack direction="row" spacing={1} alignItems="center">
                  <StatusChip label={query.data.status} />
                  {!live && (
                    <Typography variant="caption" color="text.secondary">
                      {progressHint(query.data.status)}
                    </Typography>
                  )}
                </Stack>
              </DetailField>
              <DetailField label="Service" mono>
                {query.data.serviceId ?? query.data.applicationId}
              </DetailField>
              <DetailField label="Host" mono>
                <Link component={RouterLink} to={`/hosts/${query.data.hostId}`} underline="hover">
                  {query.data.hostId}
                </Link>
              </DetailField>
              <DetailField label="Started">
                {query.data.startedAt ? new Date(query.data.startedAt).toLocaleString() : '—'}
              </DetailField>
              <DetailField label="Finished">
                {query.data.finishedAt ? new Date(query.data.finishedAt).toLocaleString() : '—'}
              </DetailField>
              <DetailField label="Created">{new Date(query.data.createdAt).toLocaleString()}</DetailField>
            </DetailPanel>

            <LogViewer logs={query.data.logs} live={live} />
          </Stack>
        )}
      </QueryState>
    </PageShell>
  )
}

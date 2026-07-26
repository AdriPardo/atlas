import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { Box, Button, Link, Stack, Typography } from '@mui/material'
import { deploymentsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'

const ACTIVE = new Set(['PENDING', 'RUNNING'])

export function DeploymentDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: ['deployments', id],
    queryFn: () => deploymentsApi.get(id),
    enabled: !!id,
    refetchInterval: (q) => {
      const status = q.state.data?.status
      return status && ACTIVE.has(status) ? 3000 : false
    },
  })

  return (
    <PageShell maxWidth={860}>
      <PageHeader
        title="Deployment"
        description={query.data ? `Record ${query.data.id.slice(0, 8)}` : 'Deployment record'}
        actions={
          <Button component={RouterLink} to="/deployments">
            Back
          </Button>
        }
      />

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <Stack spacing={3}>
            <DetailPanel>
              <DetailField label="Status">
                <StatusChip label={query.data.status} />
              </DetailField>
              <DetailField label="Application" mono>
                <Link component={RouterLink} to={`/applications/${query.data.applicationId}`} underline="hover">
                  {query.data.applicationId}
                </Link>
              </DetailField>
              <DetailField label="Host" mono>
                <Link component={RouterLink} to={`/hosts/${query.data.hostId}`} underline="hover">
                  {query.data.hostId}
                </Link>
              </DetailField>
              <DetailField label="Started">
                {query.data.startedAt ? new Date(query.data.startedAt).toLocaleString() : '-'}
              </DetailField>
              <DetailField label="Finished">
                {query.data.finishedAt ? new Date(query.data.finishedAt).toLocaleString() : '-'}
              </DetailField>
              <DetailField label="Created">{new Date(query.data.createdAt).toLocaleString()}</DetailField>
            </DetailPanel>

            <Box
              sx={{
                border: (t) => `1px solid ${t.palette.divider}`,
                borderRadius: 2,
                bgcolor: 'background.paper',
                p: { xs: 2.5, sm: 3 },
              }}
            >
              <Typography variant="subtitle1" sx={{ mb: 1.5 }}>
                Logs {ACTIVE.has(query.data.status) ? '(live)' : ''}
              </Typography>
              <Box
                component="pre"
                sx={{
                  m: 0,
                  p: 2,
                  borderRadius: 1,
                  bgcolor: 'grey.900',
                  color: 'grey.100',
                  fontFamily: '"IBM Plex Mono", monospace',
                  fontSize: 13,
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  maxHeight: 480,
                  overflow: 'auto',
                }}
              >
                {query.data.logs || 'Waiting for worker output…'}
              </Box>
            </Box>
          </Stack>
        )}
      </QueryState>
    </PageShell>
  )
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  Link,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { pipelinesApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'

export function PipelineDetailPage() {
  const { id = '' } = useParams()
  const queryClient = useQueryClient()

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

  const runs = runsQuery.data?.content ?? []

  return (
    <PageShell maxWidth={960}>
      <PageHeader
        title={query.data?.name ?? 'Pipeline'}
        description="Manual deploy pipeline — run enqueues DEPLOY_SERVICE."
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

            <Stack spacing={1}>
              <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
                Runs
              </Typography>
              <QueryState isLoading={runsQuery.isLoading} isError={runsQuery.isError}>
                {runs.length === 0 ? (
                  <EmptyState
                    title="No runs yet"
                    description="Click Run now to enqueue a deploy for this pipeline."
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

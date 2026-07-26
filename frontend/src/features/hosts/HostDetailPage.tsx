import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import { Alert, Button, Stack } from '@mui/material'
import { hostsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'

export function HostDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['hosts', id],
    queryFn: () => hostsApi.get(id),
    enabled: !!id,
  })

  const syncMutation = useMutation({
    mutationFn: () => hostsApi.sync(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['hosts', id] })
      await queryClient.invalidateQueries({ queryKey: ['jobs'] })
    },
  })

  return (
    <PageShell maxWidth={760}>
      <PageHeader
        title={query.data?.hostname ?? 'Host'}
        description="Server inventory record."
        actions={
          <Stack direction="row" spacing={1}>
            <Button component={RouterLink} to="/hosts">
              Back
            </Button>
            <Button
              variant="outlined"
              onClick={() => syncMutation.mutate()}
              disabled={syncMutation.isPending}
            >
              Sync
            </Button>
            <Button variant="contained" onClick={() => navigate(`/hosts/${id}/edit`)}>
              Edit
            </Button>
          </Stack>
        }
      />

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <Stack spacing={2}>
            {syncMutation.isSuccess && (
              <Alert severity="success" variant="outlined">
                Sync job queued ({syncMutation.data.id.slice(0, 8)}). Refresh shortly for Docker/OS metadata.
              </Alert>
            )}
            {syncMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to queue sync job
              </Alert>
            )}
            <DetailPanel>
              <DetailField label="Status">
                <StatusChip label={query.data.online ? 'ONLINE' : 'OFFLINE'} />
              </DetailField>
              <DetailField label="Connection">{query.data.connectionType}</DetailField>
              <DetailField label="IP" mono>
                {query.data.ip}
              </DetailField>
              <DetailField label="SSH user">{query.data.sshUser || '-'}</DetailField>
              <DetailField label="SSH port">{query.data.sshPort}</DetailField>
              <DetailField label="SSH secret" mono>
                {query.data.sshPrivateKeySecretId || '-'}
              </DetailField>
              <DetailField label="Operating system">{query.data.operatingSystem}</DetailField>
              <DetailField label="Docker version" mono>
                {query.data.dockerVersion || '-'}
              </DetailField>
              <DetailField label="Created">{new Date(query.data.createdAt).toLocaleString()}</DetailField>
            </DetailPanel>
          </Stack>
        )}
      </QueryState>
    </PageShell>
  )
}

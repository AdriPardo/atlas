import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import { Button, Stack } from '@mui/material'
import { hostsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'

export function HostDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const query = useQuery({
    queryKey: ['hosts', id],
    queryFn: () => hostsApi.get(id),
    enabled: !!id,
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
            <Button variant="contained" onClick={() => navigate(`/hosts/${id}/edit`)}>
              Edit
            </Button>
          </Stack>
        }
      />

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <DetailPanel>
            <DetailField label="Status">
              <StatusChip label={query.data.online ? 'ONLINE' : 'OFFLINE'} />
            </DetailField>
            <DetailField label="IP" mono>
              {query.data.ip}
            </DetailField>
            <DetailField label="Operating system">{query.data.operatingSystem}</DetailField>
            <DetailField label="Docker version" mono>
              {query.data.dockerVersion || '-'}
            </DetailField>
            <DetailField label="Created">{new Date(query.data.createdAt).toLocaleString()}</DetailField>
          </DetailPanel>
        )}
      </QueryState>
    </PageShell>
  )
}

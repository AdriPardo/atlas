import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import { Button, Stack } from '@mui/material'
import { applicationsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'

export function ApplicationDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const query = useQuery({
    queryKey: ['applications', id],
    queryFn: () => applicationsApi.get(id),
    enabled: !!id,
  })

  return (
    <PageShell maxWidth={760}>
      <PageHeader
        title={query.data?.name ?? 'Application'}
        description="Definition and repository metadata."
        actions={
          <Stack direction="row" spacing={1}>
            <Button component={RouterLink} to="/applications">
              Back
            </Button>
            <Button variant="contained" onClick={() => navigate(`/applications/${id}/edit`)}>
              Edit
            </Button>
          </Stack>
        }
      />

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <DetailPanel>
            <DetailField label="Status">
              <StatusChip label={query.data.status} />
            </DetailField>
            <DetailField label="Description">{query.data.description || 'No description'}</DetailField>
            <DetailField label="Repository" mono>
              {query.data.repositoryUrl}
            </DetailField>
            <DetailField label="Branch" mono>
              {query.data.branch}
            </DetailField>
            <DetailField label="Compose path" mono>
              {query.data.composePath}
            </DetailField>
            <DetailField label="Domain">{query.data.domain || '-'}</DetailField>
            <DetailField label="Created">{new Date(query.data.createdAt).toLocaleString()}</DetailField>
            <DetailField label="Updated">{new Date(query.data.updatedAt).toLocaleString()}</DetailField>
          </DetailPanel>
        )}
      </QueryState>
    </PageShell>
  )
}

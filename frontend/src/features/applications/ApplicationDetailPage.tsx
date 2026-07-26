import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import { Box, Button, Chip, Paper, Stack, Typography } from '@mui/material'
import { applicationsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'

export function ApplicationDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const query = useQuery({
    queryKey: ['applications', id],
    queryFn: () => applicationsApi.get(id),
    enabled: !!id,
  })

  return (
    <Stack spacing={3}>
      <Box display="flex" justifyContent="space-between" gap={2} flexWrap="wrap">
        <Typography variant="h4">Application detail</Typography>
        <Box>
          <Button component={RouterLink} to="/applications" sx={{ mr: 1 }}>
            Back
          </Button>
          <Button variant="contained" onClick={() => navigate(`/applications/${id}/edit`)}>
            Edit
          </Button>
        </Box>
      </Box>

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <Paper sx={{ p: 3 }}>
            <Stack spacing={1.5}>
              <Typography variant="h5">{query.data.name}</Typography>
              <Chip label={query.data.status} sx={{ width: 'fit-content' }} />
              <Typography color="text.secondary">{query.data.description || 'No description'}</Typography>
              <Typography>
                <strong>Repository:</strong> {query.data.repositoryUrl}
              </Typography>
              <Typography>
                <strong>Branch:</strong> {query.data.branch}
              </Typography>
              <Typography>
                <strong>Compose path:</strong> {query.data.composePath}
              </Typography>
              <Typography>
                <strong>Domain:</strong> {query.data.domain || '—'}
              </Typography>
              <Typography>
                <strong>Created:</strong> {new Date(query.data.createdAt).toLocaleString()}
              </Typography>
              <Typography>
                <strong>Updated:</strong> {new Date(query.data.updatedAt).toLocaleString()}
              </Typography>
            </Stack>
          </Paper>
        )}
      </QueryState>
    </Stack>
  )
}

import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'
import { Box, Button, Chip, Paper, Stack, Typography } from '@mui/material'
import { hostsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'

export function HostDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const query = useQuery({
    queryKey: ['hosts', id],
    queryFn: () => hostsApi.get(id),
    enabled: !!id,
  })

  return (
    <Stack spacing={3}>
      <Box display="flex" justifyContent="space-between" gap={2} flexWrap="wrap">
        <Typography variant="h4">Host detail</Typography>
        <Box>
          <Button component={RouterLink} to="/hosts" sx={{ mr: 1 }}>
            Back
          </Button>
          <Button variant="contained" onClick={() => navigate(`/hosts/${id}/edit`)}>
            Edit
          </Button>
        </Box>
      </Box>

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <Paper sx={{ p: 3 }}>
            <Stack spacing={1.5}>
              <Typography variant="h5">{query.data.hostname}</Typography>
              <Chip
                label={query.data.online ? 'Online' : 'Offline'}
                color={query.data.online ? 'success' : 'default'}
                sx={{ width: 'fit-content' }}
              />
              <Typography>
                <strong>IP:</strong> {query.data.ip}
              </Typography>
              <Typography>
                <strong>Operating system:</strong> {query.data.operatingSystem}
              </Typography>
              <Typography>
                <strong>Docker version:</strong> {query.data.dockerVersion || '—'}
              </Typography>
              <Typography>
                <strong>Created:</strong> {new Date(query.data.createdAt).toLocaleString()}
              </Typography>
            </Stack>
          </Paper>
        )}
      </QueryState>
    </Stack>
  )
}

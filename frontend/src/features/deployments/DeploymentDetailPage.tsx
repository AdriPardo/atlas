import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Chip,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { deploymentsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'

const schema = z.object({
  status: z.enum(['PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED']),
  logs: z.string().optional(),
})

type FormValues = z.infer<typeof schema>

export function DeploymentDetailPage() {
  const { id = '' } = useParams()
  const queryClient = useQueryClient()
  const query = useQuery({
    queryKey: ['deployments', id],
    queryFn: () => deploymentsApi.get(id),
    enabled: !!id,
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { status: 'PENDING', logs: '' },
  })

  useEffect(() => {
    if (query.data) {
      reset({ status: query.data.status, logs: query.data.logs })
    }
  }, [query.data, reset])

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      deploymentsApi.update(id, {
        status: values.status,
        logs: values.logs ?? '',
        startedAt:
          values.status === 'RUNNING' || values.status === 'SUCCEEDED' || values.status === 'FAILED'
            ? query.data?.startedAt ?? new Date().toISOString()
            : query.data?.startedAt,
        finishedAt:
          values.status === 'SUCCEEDED' || values.status === 'FAILED' || values.status === 'CANCELLED'
            ? new Date().toISOString()
            : null,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['deployments', id] })
      await queryClient.invalidateQueries({ queryKey: ['deployments'] })
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await mutation.mutateAsync(values)
  })

  return (
    <Stack spacing={3}>
      <Box display="flex" justifyContent="space-between" gap={2} flexWrap="wrap">
        <Typography variant="h4">Deployment detail</Typography>
        <Button component={RouterLink} to="/deployments">
          Back
        </Button>
      </Box>

      <QueryState isLoading={query.isLoading} isError={query.isError}>
        {query.data && (
          <>
            <Paper sx={{ p: 3 }}>
              <Stack spacing={1.5}>
                <Chip label={query.data.status} sx={{ width: 'fit-content' }} />
                <Typography>
                  <strong>Application:</strong>{' '}
                  <RouterLink to={`/applications/${query.data.applicationId}`}>
                    {query.data.applicationId}
                  </RouterLink>
                </Typography>
                <Typography>
                  <strong>Host:</strong>{' '}
                  <RouterLink to={`/hosts/${query.data.hostId}`}>{query.data.hostId}</RouterLink>
                </Typography>
                <Typography>
                  <strong>Started:</strong>{' '}
                  {query.data.startedAt ? new Date(query.data.startedAt).toLocaleString() : '—'}
                </Typography>
                <Typography>
                  <strong>Finished:</strong>{' '}
                  {query.data.finishedAt ? new Date(query.data.finishedAt).toLocaleString() : '—'}
                </Typography>
                <Typography>
                  <strong>Created:</strong> {new Date(query.data.createdAt).toLocaleString()}
                </Typography>
              </Stack>
            </Paper>

            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Update status (manual)
              </Typography>
              {mutation.isError && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  Unable to update deployment
                </Alert>
              )}
              {mutation.isSuccess && (
                <Alert severity="success" sx={{ mb: 2 }}>
                  Deployment updated
                </Alert>
              )}
              <Box component="form" onSubmit={onSubmit}>
                <Stack spacing={2}>
                  <TextField select label="Status" defaultValue={query.data.status} {...register('status')}>
                    {['PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'].map((status) => (
                      <MenuItem key={status} value={status}>
                        {status}
                      </MenuItem>
                    ))}
                  </TextField>
                  <TextField
                    label="Logs"
                    multiline
                    minRows={4}
                    {...register('logs')}
                    sx={{ fontFamily: '"IBM Plex Mono", monospace' }}
                  />
                  <Button type="submit" variant="contained" disabled={isSubmitting || mutation.isPending}>
                    Save status
                  </Button>
                </Stack>
              </Box>
            </Paper>
          </>
        )}
      </QueryState>
    </Stack>
  )
}

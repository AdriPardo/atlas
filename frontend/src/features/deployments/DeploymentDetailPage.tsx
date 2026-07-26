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
  Link,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { deploymentsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { StatusChip } from '../../shared/components/StatusChip'

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
    <PageShell maxWidth={760}>
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
              component="form"
              onSubmit={onSubmit}
              sx={{
                border: (t) => `1px solid ${t.palette.divider}`,
                borderRadius: 2,
                bgcolor: 'background.paper',
                p: { xs: 2.5, sm: 3 },
              }}
            >
              <Typography variant="subtitle1" sx={{ mb: 2 }}>
                Update status
              </Typography>
              {mutation.isError && (
                <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
                  Unable to update deployment
                </Alert>
              )}
              {mutation.isSuccess && (
                <Alert severity="success" variant="outlined" sx={{ mb: 2 }}>
                  Deployment updated
                </Alert>
              )}
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
                  InputProps={{
                    sx: { fontFamily: '"IBM Plex Mono", monospace', fontSize: 13 },
                  }}
                />
                <Box>
                  <Button type="submit" variant="contained" disabled={isSubmitting || mutation.isPending}>
                    Save status
                  </Button>
                </Box>
              </Stack>
            </Box>
          </Stack>
        )}
      </QueryState>
    </PageShell>
  )
}

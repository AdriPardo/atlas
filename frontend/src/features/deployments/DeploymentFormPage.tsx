import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Alert, Box, Button, MenuItem, Paper, Stack, TextField, Typography } from '@mui/material'
import { applicationsApi, deploymentsApi, hostsApi } from '../../shared/api/endpoints'

const schema = z.object({
  applicationId: z.string().uuid(),
  hostId: z.string().uuid(),
})

type FormValues = z.infer<typeof schema>

export function DeploymentFormPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const appsQuery = useQuery({
    queryKey: ['applications', 'options'],
    queryFn: () => applicationsApi.list({ page: 0, size: 100 }),
  })
  const hostsQuery = useQuery({
    queryKey: ['hosts', 'options'],
    queryFn: () => hostsApi.list({ page: 0, size: 100 }),
  })

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  const mutation = useMutation({
    mutationFn: (values: FormValues) => deploymentsApi.create(values),
    onSuccess: async (deployment) => {
      await queryClient.invalidateQueries({ queryKey: ['deployments'] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
      navigate(`/deployments/${deployment.id}`)
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await mutation.mutateAsync(values)
  })

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Create deployment</Typography>
      <Paper sx={{ p: 3, maxWidth: 720 }}>
        {mutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            Unable to create deployment
          </Alert>
        )}
        <Box component="form" onSubmit={onSubmit}>
          <Stack spacing={2}>
            <TextField
              select
              label="Application"
              defaultValue=""
              error={!!errors.applicationId}
              helperText={errors.applicationId?.message}
              {...register('applicationId')}
            >
              {(appsQuery.data?.content ?? []).map((app) => (
                <MenuItem key={app.id} value={app.id}>
                  {app.name}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Host"
              defaultValue=""
              error={!!errors.hostId}
              helperText={errors.hostId?.message}
              {...register('hostId')}
            >
              {(hostsQuery.data?.content ?? []).map((host) => (
                <MenuItem key={host.id} value={host.id}>
                  {host.hostname}
                </MenuItem>
              ))}
            </TextField>
            <Box display="flex" gap={1}>
              <Button type="submit" variant="contained" disabled={isSubmitting || mutation.isPending}>
                Create
              </Button>
              <Button onClick={() => navigate(-1)}>Cancel</Button>
            </Box>
          </Stack>
        </Box>
      </Paper>
    </Stack>
  )
}

import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Alert, Box, Button, MenuItem, Stack, TextField } from '@mui/material'
import { deploymentsApi, hostsApi, servicesApi } from '../../shared/api/endpoints'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'

import { useAuthQuery } from '../auth/useAuthQuery'
const schema = z.object({
  serviceId: z.string().uuid(),
  hostId: z.string().uuid(),
})

type FormValues = z.infer<typeof schema>

export function DeploymentFormPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const servicesQuery = useAuthQuery({
    queryKey: ['services', 'options'],
    queryFn: () => servicesApi.list({ page: 0, size: 100 }),
  })
  const hostsQuery = useAuthQuery({
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
    <PageShell maxWidth={720}>
      <PageHeader
        title="Create deployment"
        description="Link a service to a host for a manual deployment record."
      />
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
        {mutation.isError && (
          <Alert severity="error" variant="outlined" sx={{ mb: 2 }}>
            Unable to create deployment
          </Alert>
        )}
        <Stack spacing={2}>
          <TextField
            select
            label="Service"
            defaultValue=""
            error={!!errors.serviceId}
            helperText={errors.serviceId?.message}
            {...register('serviceId')}
          >
            {(servicesQuery.data?.content ?? []).map((svc) => (
              <MenuItem key={svc.id} value={svc.id}>
                {svc.name} ({svc.repositoryUrl})
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
          <Box display="flex" gap={1} pt={0.5}>
            <Button type="submit" variant="contained" disabled={isSubmitting || mutation.isPending}>
              Create
            </Button>
            <Button onClick={() => navigate(-1)}>Cancel</Button>
          </Box>
        </Stack>
      </Box>
    </PageShell>
  )
}

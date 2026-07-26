import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  FormControlLabel,
  Paper,
  Stack,
  Switch,
  TextField,
  Typography,
} from '@mui/material'
import { hostsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'

const schema = z.object({
  hostname: z.string().min(1).max(255),
  ip: z.string().min(1).max(64),
  operatingSystem: z.string().min(1).max(150),
  dockerVersion: z.string().max(100).optional(),
  online: z.boolean(),
})

type FormValues = z.infer<typeof schema>

export function HostFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const detailQuery = useQuery({
    queryKey: ['hosts', id],
    queryFn: () => hostsApi.get(id!),
    enabled: isEdit,
  })

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      hostname: '',
      ip: '',
      operatingSystem: '',
      dockerVersion: '',
      online: false,
    },
  })

  useEffect(() => {
    if (detailQuery.data) {
      reset({
        hostname: detailQuery.data.hostname,
        ip: detailQuery.data.ip,
        operatingSystem: detailQuery.data.operatingSystem,
        dockerVersion: detailQuery.data.dockerVersion,
        online: detailQuery.data.online,
      })
    }
  }, [detailQuery.data, reset])

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      isEdit ? hostsApi.update(id!, values) : hostsApi.create(values),
    onSuccess: async (host) => {
      await queryClient.invalidateQueries({ queryKey: ['hosts'] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
      navigate(`/hosts/${host.id}`)
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await mutation.mutateAsync(values)
  })

  return (
    <Stack spacing={3}>
      <Typography variant="h4">{isEdit ? 'Edit host' : 'Create host'}</Typography>
      <QueryState isLoading={isEdit && detailQuery.isLoading} isError={isEdit && detailQuery.isError}>
        <Paper sx={{ p: 3, maxWidth: 720 }}>
          {mutation.isError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Unable to save host
            </Alert>
          )}
          <Box component="form" onSubmit={onSubmit}>
            <Stack spacing={2}>
              <TextField
                label="Hostname"
                error={!!errors.hostname}
                helperText={errors.hostname?.message}
                {...register('hostname')}
              />
              <TextField
                label="IP"
                error={!!errors.ip}
                helperText={errors.ip?.message}
                {...register('ip')}
              />
              <TextField
                label="Operating system"
                error={!!errors.operatingSystem}
                helperText={errors.operatingSystem?.message}
                {...register('operatingSystem')}
              />
              <TextField
                label="Docker version"
                error={!!errors.dockerVersion}
                helperText={errors.dockerVersion?.message}
                {...register('dockerVersion')}
              />
              <Controller
                name="online"
                control={control}
                render={({ field }) => (
                  <FormControlLabel
                    control={<Switch checked={field.value} onChange={(_, checked) => field.onChange(checked)} />}
                    label="Online"
                  />
                )}
              />
              <Box display="flex" gap={1}>
                <Button type="submit" variant="contained" disabled={isSubmitting || mutation.isPending}>
                  Save
                </Button>
                <Button onClick={() => navigate(-1)}>Cancel</Button>
              </Box>
            </Stack>
          </Box>
        </Paper>
      </QueryState>
    </Stack>
  )
}

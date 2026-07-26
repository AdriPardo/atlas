import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { Alert, Box, Button, MenuItem, Stack, TextField } from '@mui/material'
import { applicationsApi } from '../../shared/api/endpoints'
import type { ApplicationStatus } from '../../shared/types/api'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'

const statuses: ApplicationStatus[] = [
  'REGISTERED',
  'READY',
  'DEPLOYING',
  'RUNNING',
  'STOPPED',
  'FAILED',
]

const schema = z.object({
  name: z.string().min(1).max(150),
  description: z.string().max(1000).optional(),
  repositoryUrl: z.string().min(1).max(500),
  branch: z.string().min(1).max(200),
  composePath: z.string().min(1).max(500),
  domain: z.string().max(255).optional(),
  status: z.enum(['REGISTERED', 'READY', 'DEPLOYING', 'RUNNING', 'STOPPED', 'FAILED']),
})

type FormValues = z.infer<typeof schema>

export function ApplicationFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const detailQuery = useQuery({
    queryKey: ['applications', id],
    queryFn: () => applicationsApi.get(id!),
    enabled: isEdit,
  })

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      description: '',
      repositoryUrl: '',
      branch: 'main',
      composePath: './docker-compose.yml',
      domain: '',
      status: 'REGISTERED',
    },
  })

  useEffect(() => {
    if (detailQuery.data) {
      reset({
        name: detailQuery.data.name,
        description: detailQuery.data.description,
        repositoryUrl: detailQuery.data.repositoryUrl,
        branch: detailQuery.data.branch,
        composePath: detailQuery.data.composePath,
        domain: detailQuery.data.domain,
        status: detailQuery.data.status,
      })
    }
  }, [detailQuery.data, reset])

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      isEdit ? applicationsApi.update(id!, values) : applicationsApi.create(values),
    onSuccess: async (app) => {
      await queryClient.invalidateQueries({ queryKey: ['applications'] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
      navigate(`/applications/${app.id}`)
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await mutation.mutateAsync(values)
  })

  return (
    <PageShell maxWidth={720}>
      <PageHeader
        title={isEdit ? 'Edit application' : 'Create application'}
        description={isEdit ? 'Update repository and compose settings.' : 'Register a new application definition.'}
      />
      <QueryState
        isLoading={isEdit && detailQuery.isLoading}
        isError={isEdit && detailQuery.isError}
      >
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
              Unable to save application
            </Alert>
          )}
          <Stack spacing={2}>
            <TextField
              label="Name"
              error={!!errors.name}
              helperText={errors.name?.message}
              {...register('name')}
            />
            <TextField
              label="Description"
              multiline
              minRows={2}
              error={!!errors.description}
              helperText={errors.description?.message}
              {...register('description')}
            />
            <TextField
              label="Repository URL"
              error={!!errors.repositoryUrl}
              helperText={errors.repositoryUrl?.message}
              {...register('repositoryUrl')}
            />
            <TextField
              label="Branch"
              error={!!errors.branch}
              helperText={errors.branch?.message}
              {...register('branch')}
            />
            <TextField
              label="Compose path"
              error={!!errors.composePath}
              helperText={errors.composePath?.message}
              {...register('composePath')}
            />
            <TextField
              label="Domain"
              error={!!errors.domain}
              helperText={errors.domain?.message}
              {...register('domain')}
            />
            {isEdit && (
              <TextField select label="Status" defaultValue="REGISTERED" {...register('status')}>
                {statuses.map((status) => (
                  <MenuItem key={status} value={status}>
                    {status}
                  </MenuItem>
                ))}
              </TextField>
            )}
            <Box display="flex" gap={1} pt={0.5}>
              <Button type="submit" variant="contained" disabled={isSubmitting || mutation.isPending}>
                Save
              </Button>
              <Button onClick={() => navigate(-1)}>Cancel</Button>
            </Box>
          </Stack>
        </Box>
      </QueryState>
    </PageShell>
  )
}

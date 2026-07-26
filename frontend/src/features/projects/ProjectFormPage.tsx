import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { Alert, Box, Button, MenuItem, Stack, TextField } from '@mui/material'
import { projectsApi, servicesApi } from '../../shared/api/endpoints'
import type { ProjectStatus } from '../../shared/types/api'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'

const statuses: ProjectStatus[] = [
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

export function ProjectFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const detailQuery = useQuery({
    queryKey: ['projects', id],
    queryFn: () => projectsApi.get(id!),
    enabled: isEdit,
  })

  const servicesQuery = useQuery({
    queryKey: ['projects', id, 'services'],
    queryFn: () => projectsApi.listServices(id!, { size: 10 }),
    enabled: isEdit,
  })

  const defaultService = servicesQuery.data?.content?.[0]

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
    if (detailQuery.data && defaultService) {
      reset({
        name: detailQuery.data.name,
        description: detailQuery.data.description,
        repositoryUrl: defaultService.repositoryUrl,
        branch: defaultService.branch,
        composePath: defaultService.composePath,
        domain: defaultService.domain,
        status: detailQuery.data.status,
      })
    }
  }, [detailQuery.data, defaultService, reset])

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      if (!isEdit) {
        return projectsApi.create(values)
      }
      const project = await projectsApi.update(id!, {
        name: values.name,
        description: values.description,
        status: values.status,
      })
      if (defaultService) {
        await servicesApi.update(defaultService.id, {
          name: defaultService.name,
          repositoryUrl: values.repositoryUrl,
          branch: values.branch,
          composePath: values.composePath,
          domain: values.domain,
          environment: defaultService.environment,
          status: values.status,
        })
      }
      return project
    },
    onSuccess: async (project) => {
      await queryClient.invalidateQueries({ queryKey: ['projects'] })
      await queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
      navigate(`/projects/${project.id}`)
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    await mutation.mutateAsync(values)
  })

  return (
    <PageShell maxWidth={720}>
      <PageHeader
        title={isEdit ? 'Edit project' : 'Create project'}
        description={
          isEdit
            ? 'Update project metadata and default service settings.'
            : 'Creates a project with a default deployable service.'
        }
      />
      <QueryState
        isLoading={isEdit && (detailQuery.isLoading || servicesQuery.isLoading)}
        isError={isEdit && (detailQuery.isError || servicesQuery.isError)}
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
              Unable to save project
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

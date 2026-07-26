import { zodResolver } from '@hookform/resolvers/zod';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import {
  createApplication,
  getApplication,
  updateApplication,
} from '../../api/applications';
import {
  ErrorState,
  LoadingState,
  PageHeader,
  getErrorMessage,
} from '../../components/PageHelpers';
import type { ApplicationStatus, CreateApplicationRequest, UpdateApplicationRequest } from '../../types';

const baseSchema = z.object({
  name: z.string().min(1, 'El nombre es obligatorio').max(120),
  description: z.string().max(1000).optional().or(z.literal('')),
  repositoryUrl: z.string().min(1, 'La URL del repositorio es obligatoria').max(500),
  branch: z.string().max(120).optional().or(z.literal('')),
  composePath: z.string().max(255).optional().or(z.literal('')),
  domain: z.string().max(255).optional().or(z.literal('')),
});

const editSchema = baseSchema.extend({
  branch: z.string().min(1, 'La rama es obligatoria').max(120),
  composePath: z.string().min(1, 'La ruta Compose es obligatoria').max(255),
  status: z.enum(['DRAFT', 'READY', 'DEPLOYING', 'RUNNING', 'FAILED', 'STOPPED']),
});

type CreateFormValues = z.infer<typeof baseSchema>;
type EditFormValues = z.infer<typeof editSchema>;

const STATUS_OPTIONS: Array<{ value: ApplicationStatus; label: string }> = [
  { value: 'DRAFT', label: 'Borrador' },
  { value: 'READY', label: 'Lista' },
  { value: 'DEPLOYING', label: 'Desplegando' },
  { value: 'RUNNING', label: 'En ejecución' },
  { value: 'FAILED', label: 'Fallida' },
  { value: 'STOPPED', label: 'Detenida' },
];

export function ApplicationCreatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CreateFormValues>({
    resolver: zodResolver(baseSchema),
    defaultValues: {
      name: '',
      description: '',
      repositoryUrl: '',
      branch: 'main',
      composePath: 'docker-compose.yml',
      domain: '',
    },
  });

  const mutation = useMutation({
    mutationFn: (payload: CreateApplicationRequest) => createApplication(payload),
    onSuccess: async (app) => {
      await queryClient.invalidateQueries({ queryKey: ['applications'] });
      navigate(`/applications/${app.id}`);
    },
  });

  const onSubmit = handleSubmit((values) => {
    mutation.mutate({
      name: values.name,
      description: values.description || undefined,
      repositoryUrl: values.repositoryUrl,
      branch: values.branch || undefined,
      composePath: values.composePath || undefined,
      domain: values.domain || undefined,
    });
  });

  return (
    <>
      <PageHeader
        title="Nueva aplicación"
        subtitle="Registra una aplicación para desplegarla en Atlas."
      />
      <Paper sx={{ p: 3, maxWidth: 720 }}>
        {mutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {getErrorMessage(mutation.error, 'No se pudo crear la aplicación.')}
          </Alert>
        )}
        <Box component="form" onSubmit={onSubmit} noValidate>
          <Stack spacing={2}>
            <TextField
              label="Nombre"
              fullWidth
              required
              error={Boolean(errors.name)}
              helperText={errors.name?.message}
              {...register('name')}
            />
            <TextField
              label="Descripción"
              fullWidth
              multiline
              minRows={2}
              error={Boolean(errors.description)}
              helperText={errors.description?.message}
              {...register('description')}
            />
            <TextField
              label="URL del repositorio"
              fullWidth
              required
              error={Boolean(errors.repositoryUrl)}
              helperText={errors.repositoryUrl?.message}
              {...register('repositoryUrl')}
            />
            <TextField
              label="Rama"
              fullWidth
              error={Boolean(errors.branch)}
              helperText={errors.branch?.message}
              {...register('branch')}
            />
            <TextField
              label="Ruta Compose"
              fullWidth
              error={Boolean(errors.composePath)}
              helperText={errors.composePath?.message}
              {...register('composePath')}
            />
            <TextField
              label="Dominio"
              fullWidth
              error={Boolean(errors.domain)}
              helperText={errors.domain?.message}
              {...register('domain')}
            />
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button onClick={() => navigate('/applications')}>Cancelar</Button>
              <Button
                type="submit"
                variant="contained"
                disabled={isSubmitting || mutation.isPending}
                startIcon={
                  isSubmitting || mutation.isPending ? (
                    <CircularProgress size={18} color="inherit" />
                  ) : null
                }
              >
                Crear
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Paper>
    </>
  );
}

export function ApplicationEditPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['applications', id],
    queryFn: () => getApplication(id),
    enabled: Boolean(id),
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<EditFormValues>({
    resolver: zodResolver(editSchema),
    defaultValues: {
      name: '',
      description: '',
      repositoryUrl: '',
      branch: '',
      composePath: '',
      domain: '',
      status: 'DRAFT',
    },
  });

  useEffect(() => {
    if (data) {
      reset({
        name: data.name,
        description: data.description ?? '',
        repositoryUrl: data.repositoryUrl,
        branch: data.branch,
        composePath: data.composePath,
        domain: data.domain ?? '',
        status: data.status,
      });
    }
  }, [data, reset]);

  const mutation = useMutation({
    mutationFn: (payload: UpdateApplicationRequest) => updateApplication(id, payload),
    onSuccess: async (app) => {
      await queryClient.invalidateQueries({ queryKey: ['applications'] });
      navigate(`/applications/${app.id}`);
    },
  });

  if (isLoading) return <LoadingState label="Cargando aplicación…" />;
  if (isError || !data) {
    return (
      <ErrorState
        message={getErrorMessage(error, 'No se pudo cargar la aplicación.')}
        action={
          <Button color="inherit" size="small" onClick={() => refetch()}>
            Reintentar
          </Button>
        }
      />
    );
  }

  const onSubmit = handleSubmit((values) => {
    mutation.mutate({
      name: values.name,
      description: values.description || undefined,
      repositoryUrl: values.repositoryUrl,
      branch: values.branch,
      composePath: values.composePath,
      domain: values.domain || undefined,
      status: values.status,
    });
  });

  return (
    <>
      <PageHeader
        title={`Editar: ${data.name}`}
        subtitle="Actualiza la configuración de la aplicación."
      />
      <Paper sx={{ p: 3, maxWidth: 720 }}>
        {mutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {getErrorMessage(mutation.error, 'No se pudo actualizar la aplicación.')}
          </Alert>
        )}
        <Box component="form" onSubmit={onSubmit} noValidate>
          <Stack spacing={2}>
            <TextField
              label="Nombre"
              fullWidth
              required
              error={Boolean(errors.name)}
              helperText={errors.name?.message}
              {...register('name')}
            />
            <TextField
              label="Descripción"
              fullWidth
              multiline
              minRows={2}
              error={Boolean(errors.description)}
              helperText={errors.description?.message}
              {...register('description')}
            />
            <TextField
              label="URL del repositorio"
              fullWidth
              required
              error={Boolean(errors.repositoryUrl)}
              helperText={errors.repositoryUrl?.message}
              {...register('repositoryUrl')}
            />
            <TextField
              label="Rama"
              fullWidth
              required
              error={Boolean(errors.branch)}
              helperText={errors.branch?.message}
              {...register('branch')}
            />
            <TextField
              label="Ruta Compose"
              fullWidth
              required
              error={Boolean(errors.composePath)}
              helperText={errors.composePath?.message}
              {...register('composePath')}
            />
            <TextField
              label="Dominio"
              fullWidth
              error={Boolean(errors.domain)}
              helperText={errors.domain?.message}
              {...register('domain')}
            />
            <FormControl fullWidth error={Boolean(errors.status)}>
              <InputLabel id="status-label">Estado</InputLabel>
              <Controller
                name="status"
                control={control}
                render={({ field }) => (
                  <Select labelId="status-label" label="Estado" {...field}>
                    {STATUS_OPTIONS.map((option) => (
                      <MenuItem key={option.value} value={option.value}>
                        {option.label}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
            </FormControl>
            <Stack direction="row" spacing={1} justifyContent="flex-end">
              <Button onClick={() => navigate(`/applications/${id}`)}>Cancelar</Button>
              <Button
                type="submit"
                variant="contained"
                disabled={isSubmitting || mutation.isPending}
                startIcon={
                  isSubmitting || mutation.isPending ? (
                    <CircularProgress size={18} color="inherit" />
                  ) : null
                }
              >
                Guardar
              </Button>
            </Stack>
          </Stack>
        </Box>
      </Paper>
    </>
  );
}

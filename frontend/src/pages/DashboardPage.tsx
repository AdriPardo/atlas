import {
  Apps as AppsIcon,
  CloudUpload as DeployIcon,
  Dns as HostIcon,
  PlayCircleOutline as RunningIcon,
} from '@mui/icons-material';
import { Box, Card, CardContent, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getDashboardStats } from '../api/dashboard';
import { ErrorState, LoadingState, PageHeader } from '../components/PageHelpers';

export function DashboardPage() {
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: getDashboardStats,
  });

  if (isLoading) return <LoadingState label="Cargando panel…" />;
  if (isError || !data) {
    return (
      <ErrorState
        message={error instanceof Error ? error.message : 'No se pudieron cargar las estadísticas.'}
        action={
          <Typography
            component="button"
            onClick={() => refetch()}
            sx={{ border: 0, bgcolor: 'transparent', cursor: 'pointer', color: 'inherit' }}
          >
            Reintentar
          </Typography>
        }
      />
    );
  }

  const cards = [
    {
      label: 'Aplicaciones',
      value: data.applications,
      icon: <AppsIcon color="primary" />,
    },
    {
      label: 'En ejecución',
      value: data.runningApplications,
      icon: <RunningIcon color="success" />,
    },
    {
      label: 'Hosts',
      value: data.hosts,
      icon: <HostIcon color="secondary" />,
    },
    {
      label: 'Despliegues',
      value: data.deployments,
      icon: <DeployIcon color="warning" />,
    },
  ];

  return (
    <>
      <PageHeader
        title="Panel"
        subtitle="Resumen operativo de la plataforma Atlas."
      />
      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: {
            xs: '1fr',
            sm: 'repeat(2, 1fr)',
            md: 'repeat(4, 1fr)',
          },
        }}
      >
        {cards.map((card) => (
          <Card key={card.label}>
            <CardContent>
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}
              >
                {card.icon}
                {card.label}
              </Typography>
              <Typography variant="h3">{card.value}</Typography>
            </CardContent>
          </Card>
        ))}
      </Box>
    </>
  );
}

import { Box, Button, Paper, Stack, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";
import { Link as RouterLink, useParams } from "react-router-dom";
import { getDeployment } from "../../api/deployments";
import { ErrorState, LoadingState, PageHeader, formatDateTime } from "../../components/PageHelpers";
import { DeploymentStatusChip } from "../../components/StatusChip";

export function DeploymentDetailPage() {
  const { id = "" } = useParams();
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["deployment", id],
    queryFn: () => getDeployment(id),
    enabled: Boolean(id),
  });

  if (isLoading) {
    return <LoadingState label="Cargando despliegue…" />;
  }

  if (isError || !data) {
    return (
      <ErrorState
        message={error instanceof Error ? error.message : "Despliegue no encontrado."}
        action={
          <Button color="inherit" size="small" onClick={() => refetch()}>
            Reintentar
          </Button>
        }
      />
    );
  }

  return (
    <>
      <PageHeader
        title="Detalle de despliegue"
        subtitle={data.id}
        actions={
          <Button component={RouterLink} to="/deployments" variant="outlined">
            Volver
          </Button>
        }
      />
      <Paper sx={{ p: 3 }}>
        <Stack spacing={2}>
          <Box>
            <Typography variant="overline" color="text.secondary">
              Estado
            </Typography>
            <Box sx={{ mt: 0.5 }}>
              <DeploymentStatusChip status={data.status} />
            </Box>
          </Box>
          <Box>
            <Typography variant="overline" color="text.secondary">
              Aplicación
            </Typography>
            <Typography fontFamily="monospace">{data.applicationId}</Typography>
          </Box>
          <Box>
            <Typography variant="overline" color="text.secondary">
              Host
            </Typography>
            <Typography fontFamily="monospace">{data.hostId}</Typography>
          </Box>
          <Box>
            <Typography variant="overline" color="text.secondary">
              Inicio
            </Typography>
            <Typography>{formatDateTime(data.startedAt)}</Typography>
          </Box>
          <Box>
            <Typography variant="overline" color="text.secondary">
              Fin
            </Typography>
            <Typography>{formatDateTime(data.finishedAt)}</Typography>
          </Box>
          <Box>
            <Typography variant="overline" color="text.secondary">
              Logs
            </Typography>
            <Paper
              variant="outlined"
              sx={{
                mt: 1,
                p: 2,
                bgcolor: "action.hover",
                fontFamily: "monospace",
                whiteSpace: "pre-wrap",
                fontSize: 13,
              }}
            >
              {data.logs || "Sin logs."}
            </Paper>
          </Box>
        </Stack>
      </Paper>
    </>
  );
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Stack, Typography } from '@mui/material'
import { projectDatabaseApi } from '../../shared/api/endpoints'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { QueryState } from '../../shared/components/QueryState'
import { StatusChip } from '../../shared/components/StatusChip'
import { DB_SCHEMA_SECRET, DB_URL_SECRET } from '../secrets/knownSecretHints'

export function ProjectDatabasePanel({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient()

  const statusQuery = useQuery({
    queryKey: ['projects', projectId, 'database'],
    queryFn: () => projectDatabaseApi.status(projectId),
    enabled: !!projectId,
  })

  const provisionMutation = useMutation({
    mutationFn: () => projectDatabaseApi.provision(projectId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'database'] })
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'secrets'] })
    },
  })

  const data = statusQuery.data

  return (
    <DetailPanel>
      <Stack spacing={1.5}>
        <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
          Database
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Isolated schema + migrator role (ADR-0015). Credentials land in project secrets{' '}
          <Typography component="span" className="atlas-mono" sx={{ fontSize: '0.85em' }}>
            {DB_URL_SECRET}
          </Typography>{' '}
          /{' '}
          <Typography component="span" className="atlas-mono" sx={{ fontSize: '0.85em' }}>
            {DB_SCHEMA_SECRET}
          </Typography>
          . Control-plane DB <code>atlas</code> is never exposed.
        </Typography>

        <QueryState isLoading={statusQuery.isLoading} isError={statusQuery.isError}>
          {data && (
            <Stack spacing={1.5}>
              <DetailField label="Status">
                <StatusChip
                  label={
                    data.provisioned
                      ? 'PROVISIONED'
                      : data.provisionerConfigured
                        ? 'READY'
                        : 'NOT_CONFIGURED'
                  }
                />
              </DetailField>
              <DetailField label="Schema" mono>
                {data.schema}
              </DetailField>
              <DetailField label="Role" mono>
                {data.role}
              </DetailField>
              <DetailField label="Profile" mono>
                {data.profile}
              </DetailField>
              {data.databaseName && (
                <DetailField label="Database" mono>
                  {data.databaseName}
                </DetailField>
              )}
              <Alert
                severity={data.provisioned ? 'success' : data.provisionerConfigured ? 'info' : 'warning'}
                variant="outlined"
              >
                {data.message}
              </Alert>
              {provisionMutation.isError && (
                <Alert severity="error" variant="outlined">
                  Provision failed. Check ATLAS_APP_DB_* points at a dedicated apps database (not
                  atlas).
                </Alert>
              )}
              {provisionMutation.isSuccess && (
                <Alert severity="success" variant="outlined">
                  {provisionMutation.data.rotated
                    ? 'Rotated migrator password and updated db.url.'
                    : 'Schema + role created; db.url / db.schema stored.'}
                </Alert>
              )}
              <Stack direction="row" spacing={1}>
                <Button
                  variant="contained"
                  disabled={!data.provisionerConfigured || provisionMutation.isPending}
                  onClick={() => provisionMutation.mutate()}
                >
                  {data.provisioned ? 'Rotate / re-provision' : 'Provision DB'}
                </Button>
              </Stack>
            </Stack>
          )}
        </QueryState>
      </Stack>
    </DetailPanel>
  )
}

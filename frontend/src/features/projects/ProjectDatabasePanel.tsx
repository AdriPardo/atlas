import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { projectDatabaseApi } from '../../shared/api/endpoints'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'
import { QueryState } from '../../shared/components/QueryState'
import { StatusChip } from '../../shared/components/StatusChip'
import { DB_SCHEMA_SECRET, DB_URL_SECRET } from '../secrets/knownSecretHints'

export function ProjectDatabasePanel({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient()
  const [profile, setProfile] = useState('db.read')
  const [ttlMinutes, setTtlMinutes] = useState(60)
  const [issuedUrl, setIssuedUrl] = useState<string | null>(null)
  const [copyLabel, setCopyLabel] = useState('Copy URL')

  const statusQuery = useQuery({
    queryKey: ['projects', projectId, 'database'],
    queryFn: () => projectDatabaseApi.status(projectId),
    enabled: !!projectId,
  })

  const credentialsQuery = useQuery({
    queryKey: ['projects', projectId, 'database', 'credentials'],
    queryFn: () => projectDatabaseApi.listCredentials(projectId),
    enabled: !!projectId && !!statusQuery.data?.provisioned,
  })

  const provisionMutation = useMutation({
    mutationFn: () => projectDatabaseApi.provision(projectId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'database'] })
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'secrets'] })
    },
  })

  const issueMutation = useMutation({
    mutationFn: () =>
      projectDatabaseApi.issueCredential(projectId, { profile, ttlMinutes }),
    onSuccess: async (result) => {
      setIssuedUrl(result.connectionUrl)
      setCopyLabel('Copy URL')
      await queryClient.invalidateQueries({
        queryKey: ['projects', projectId, 'database', 'credentials'],
      })
    },
  })

  const revokeMutation = useMutation({
    mutationFn: (role: string) => projectDatabaseApi.revokeCredential(projectId, role),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ['projects', projectId, 'database', 'credentials'],
      })
    },
  })

  const data = statusQuery.data

  async function copyUrl() {
    if (!issuedUrl) return
    await navigator.clipboard.writeText(issuedUrl)
    setCopyLabel('Copied')
  }

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
          . Control-plane DB <code>atlas</code> is never exposed. TTL URLs are ephemeral (option C)
          and do not rotate <code>db.url</code>.
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
                  {(() => {
                    const apiMessage =
                      (provisionMutation.error as { response?: { data?: { message?: string } } })
                        ?.response?.data?.message
                    return apiMessage?.trim()
                      ? apiMessage
                      : 'Provision failed. Check ATLAS_APP_DB_* points at a dedicated apps database (not atlas) and the role has CREATEROLE.'
                  })()}
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

              {data.provisioned && data.provisionerConfigured && (
                <Stack spacing={1.5} sx={{ pt: 1 }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 650 }}>
                    Temporary credentials
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Issue a short-lived URL for local <code>psql</code> / GUI. Default profile is
                    read-only.
                  </Typography>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                    <FormControl size="small" sx={{ minWidth: 160 }}>
                      <InputLabel id="db-profile-label">Profile</InputLabel>
                      <Select
                        labelId="db-profile-label"
                        label="Profile"
                        value={profile}
                        onChange={(e) => setProfile(e.target.value)}
                      >
                        <MenuItem value="db.read">db.read</MenuItem>
                        <MenuItem value="db.migrate">db.migrate</MenuItem>
                        <MenuItem value="db.admin">db.admin</MenuItem>
                      </Select>
                    </FormControl>
                    <TextField
                      size="small"
                      type="number"
                      label="TTL minutes"
                      value={ttlMinutes}
                      onChange={(e) => setTtlMinutes(Number(e.target.value) || 60)}
                      inputProps={{ min: 5, max: 1440 }}
                      sx={{ width: 140 }}
                    />
                    <Button
                      variant="outlined"
                      disabled={issueMutation.isPending}
                      onClick={() => issueMutation.mutate()}
                    >
                      Issue URL
                    </Button>
                  </Stack>
                  {issueMutation.isError && (
                    <Alert severity="error" variant="outlined">
                      Could not issue credentials. Check permissions and provisioner.
                    </Alert>
                  )}
                  {issuedUrl && (
                    <Alert severity="info" variant="outlined">
                      <Stack spacing={1}>
                        <Typography variant="body2">
                          One-shot URL (shown once). Expires after TTL; copy now.
                        </Typography>
                        <Typography
                          component="code"
                          className="atlas-mono"
                          sx={{ fontSize: 12, wordBreak: 'break-all' }}
                        >
                          {issuedUrl}
                        </Typography>
                        <Button size="small" variant="text" onClick={() => void copyUrl()}>
                          {copyLabel}
                        </Button>
                      </Stack>
                    </Alert>
                  )}
                  <QueryState
                    isLoading={credentialsQuery.isLoading}
                    isError={credentialsQuery.isError}
                  >
                    {(credentialsQuery.data?.length ?? 0) > 0 && (
                      <Stack spacing={1}>
                        <Typography variant="body2" color="text.secondary">
                          Active TTL roles
                        </Typography>
                        {credentialsQuery.data?.map((item) => (
                          <Stack
                            key={item.role}
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={1}
                            alignItems={{ sm: 'center' }}
                            justifyContent="space-between"
                          >
                            <Stack spacing={0.25}>
                              <Typography className="atlas-mono" sx={{ fontSize: 13 }}>
                                {item.role}
                              </Typography>
                              <Typography variant="caption" color="text.secondary">
                                {item.expired
                                  ? 'Expired'
                                  : item.expiresAt
                                    ? `Expires ${new Date(item.expiresAt).toLocaleString()}`
                                    : 'No expiry'}
                              </Typography>
                            </Stack>
                            <Button
                              size="small"
                              color="warning"
                              disabled={revokeMutation.isPending}
                              onClick={() => revokeMutation.mutate(item.role)}
                            >
                              Revoke
                            </Button>
                          </Stack>
                        ))}
                      </Stack>
                    )}
                  </QueryState>
                </Stack>
              )}
            </Stack>
          )}
        </QueryState>
      </Stack>
    </DetailPanel>
  )
}

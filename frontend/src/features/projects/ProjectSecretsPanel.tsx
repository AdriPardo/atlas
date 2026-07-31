import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { projectSecretsApi, secretsApi } from '../../shared/api/endpoints'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { QueryState } from '../../shared/components/QueryState'
import { RowOverflowMenu } from '../../shared/components/RowOverflowMenu'
import { StatusChip } from '../../shared/components/StatusChip'
import { CloudflareTokenScopesHint } from '../secrets/CloudflareTokenScopesHint'
import { secretNameHelperText } from '../secrets/knownSecretHints'
import { useAuth } from '../auth/AuthContext'

export function ProjectSecretsPanel({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const [name, setName] = useState('')
  const [value, setValue] = useState('')
  const [linkSecretId, setLinkSecretId] = useState('')
  const [alias, setAlias] = useState('')
  const [rotateName, setRotateName] = useState<string | null>(null)
  const [rotateValue, setRotateValue] = useState('')

  const secretsQuery = useQuery({
    queryKey: ['projects', projectId, 'secrets'],
    queryFn: () => projectSecretsApi.list(projectId),
    enabled: !!projectId,
  })

  const orgSecretsQuery = useQuery({
    queryKey: ['secrets', 'org'],
    queryFn: () => secretsApi.list(),
    enabled: !!projectId,
  })

  const saveMutation = useMutation({
    mutationFn: () => projectSecretsApi.upsert(projectId, { name: name.trim(), value }),
    onSuccess: async () => {
      setValue('')
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'secrets'] })
    },
  })

  const rotateMutation = useMutation({
    mutationFn: () =>
      projectSecretsApi.upsert(projectId, { name: rotateName!.trim(), value: rotateValue }),
    onSuccess: async () => {
      setRotateName(null)
      setRotateValue('')
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'secrets'] })
    },
  })

  const linkMutation = useMutation({
    mutationFn: () =>
      projectSecretsApi.link(projectId, {
        secretId: linkSecretId,
        alias: alias.trim() || undefined,
      }),
    onSuccess: async () => {
      setLinkSecretId('')
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'secrets'] })
    },
  })

  const removeOwnedMutation = useMutation({
    mutationFn: (secretId: string) => projectSecretsApi.removeOwned(projectId, secretId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'secrets'] })
    },
  })

  const unlinkMutation = useMutation({
    mutationFn: (bindingId: string) => projectSecretsApi.unlink(projectId, bindingId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'secrets'] })
    },
  })

  const rows = secretsQuery.data ?? []
  const orgSecrets = orgSecretsQuery.data ?? []
  const linkedIds = new Set(rows.filter((r) => r.kind === 'LINKED').map((r) => r.secretId))
  const linkable = orgSecrets.filter((s) => !linkedIds.has(s.id))

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
        Secrets
      </Typography>
      <Alert severity="info" variant="outlined">
        Create secrets here (your API keys, tokens, …). Reference them in the project{' '}
        <Typography component="span" className="atlas-mono" sx={{ fontSize: '0.85em' }}>
          atlas.yml
        </Typography>{' '}
        via{' '}
        <Typography component="span" className="atlas-mono" sx={{ fontSize: '0.85em' }}>
          envFrom.secretRef
        </Typography>
        ; deploy writes them into the workspace{' '}
        <Typography component="span" className="atlas-mono" sx={{ fontSize: '0.85em' }}>
          .env
        </Typography>{' '}
        for your app. Resolution: binding alias → project-owned → organization. OPERATOR+ can manage
        project secrets; organization secrets are ADMIN-only.
      </Alert>
      <CloudflareTokenScopesHint />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'flex-start' }}>
        <TextField
          label="Name"
          size="small"
          value={name}
          onChange={(e) => setName(e.target.value)}
          sx={{ minWidth: 140 }}
          helperText={secretNameHelperText(name)}
          placeholder="openai.api_key"
        />
        <TextField
          label="Value"
          size="small"
          type="password"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          sx={{ flex: 1, minWidth: 180 }}
        />
        <Button
          variant="contained"
          disabled={!name.trim() || !value || saveMutation.isPending}
          onClick={() => saveMutation.mutate()}
          sx={{ whiteSpace: 'nowrap' }}
        >
          Save
        </Button>
      </Stack>
      {saveMutation.isError && (
        <Alert severity="error" variant="outlined">
          Unable to save project secret (need OPERATOR membership, or name is a linked org alias)
        </Alert>
      )}

      <Stack spacing={1}>
        <Typography variant="body2" color="text.secondary">
          Link an organization secret into this project
          {isAdmin ? '' : ' (create org secrets as ADMIN under Org secrets)'}
        </Typography>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'flex-start' }}>
          <TextField
            select
            label="Organization secret"
            size="small"
            value={linkSecretId}
            onChange={(e) => setLinkSecretId(e.target.value)}
            sx={{ minWidth: 200 }}
            disabled={linkable.length === 0}
          >
            {linkable.length === 0 ? (
              <MenuItem value="" disabled>
                No org secrets available
              </MenuItem>
            ) : (
              linkable.map((s) => (
                <MenuItem key={s.id} value={s.id}>
                  {s.name}
                </MenuItem>
              ))
            )}
          </TextField>
          <TextField
            label="Alias"
            size="small"
            value={alias}
            onChange={(e) => setAlias(e.target.value)}
            sx={{ minWidth: 140 }}
            helperText={secretNameHelperText(alias, 'Logical name for deploy / envFrom')}
          />
          <Button
            variant="outlined"
            disabled={!linkSecretId || linkMutation.isPending}
            onClick={() => linkMutation.mutate()}
          >
            Link
          </Button>
          {isAdmin && (
            <Button size="small" component={RouterLink} to="/secrets" sx={{ alignSelf: 'center' }}>
              Org secrets
            </Button>
          )}
        </Stack>
        {linkMutation.isError && (
          <Alert severity="error" variant="outlined">
            Unable to link secret (need OPERATOR membership, or alias may already exist)
          </Alert>
        )}
        {orgSecretsQuery.isError && (
          <Alert severity="warning" variant="outlined">
            Could not load organization secrets.
          </Alert>
        )}
      </Stack>

      <DataTableFrame>
        <QueryState isLoading={secretsQuery.isLoading} isError={secretsQuery.isError}>
          {rows.length === 0 ? (
            <EmptyState
              title="No project secrets"
              description="Save an API key or token here, then declare envFrom.secretRef in atlas.yml so deploy injects it into your app."
            />
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name / alias</TableCell>
                  <TableCell>Kind</TableCell>
                  <TableCell>Source</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.bindingId ?? row.secretId} hover>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 600 }} className="atlas-mono">
                        {row.name}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <StatusChip label={row.kind} />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary" className="atlas-mono">
                        {row.kind === 'LINKED' ? row.secretName : 'project-owned'}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <RowOverflowMenu
                        aria-label={`Actions for ${row.name}`}
                        items={
                          row.kind === 'OWNED'
                            ? [
                                {
                                  label: 'Rotate value',
                                  disabled: rotateMutation.isPending,
                                  onClick: () => {
                                    setRotateName(row.name)
                                    setRotateValue('')
                                  },
                                },
                                {
                                  label: 'Delete',
                                  destructive: true,
                                  disabled: removeOwnedMutation.isPending,
                                  onClick: () => removeOwnedMutation.mutate(row.secretId),
                                },
                              ]
                            : [
                                {
                                  label: 'Unlink',
                                  disabled: !row.bindingId || unlinkMutation.isPending,
                                  onClick: () =>
                                    row.bindingId && unlinkMutation.mutate(row.bindingId),
                                },
                              ]
                        }
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </QueryState>
      </DataTableFrame>

      <Dialog
        open={!!rotateName}
        onClose={() => {
          setRotateName(null)
          setRotateValue('')
        }}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Rotate secret value</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {rotateMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to rotate secret
              </Alert>
            )}
            <TextField label="Name" value={rotateName ?? ''} fullWidth disabled className="atlas-mono" />
            <TextField
              label="New value"
              value={rotateValue}
              onChange={(e) => setRotateValue(e.target.value)}
              type="password"
              fullWidth
              autoFocus
              multiline
              minRows={2}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setRotateName(null)
              setRotateValue('')
            }}
          >
            Cancel
          </Button>
          <Button
            variant="contained"
            disabled={!rotateValue || rotateMutation.isPending}
            onClick={() => rotateMutation.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}

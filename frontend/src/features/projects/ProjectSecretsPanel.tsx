import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
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
import { useAuth } from '../auth/AuthContext'

export function ProjectSecretsPanel({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const [name, setName] = useState('git.token')
  const [value, setValue] = useState('')
  const [linkSecretId, setLinkSecretId] = useState('')
  const [alias, setAlias] = useState('git.token')

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

  const createMutation = useMutation({
    mutationFn: () => projectSecretsApi.create(projectId, { name: name.trim(), value }),
    onSuccess: async () => {
      setValue('')
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
        Deploy resolves <Typography component="span" className="atlas-mono" sx={{ fontSize: '0.85em' }}>
          git.token
        </Typography>{' '}
        in order: project binding alias → project-owned name → organization secret. OPERATOR+ can
        manage project secrets; organization secrets are ADMIN-only.
      </Alert>

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'flex-start' }}>
        <TextField
          label="Name"
          size="small"
          value={name}
          onChange={(e) => setName(e.target.value)}
          sx={{ minWidth: 140 }}
          helperText="e.g. git.token"
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
          disabled={!name.trim() || !value || createMutation.isPending}
          onClick={() => createMutation.mutate()}
          sx={{ whiteSpace: 'nowrap' }}
        >
          Create
        </Button>
      </Stack>
      {createMutation.isError && (
        <Alert severity="error" variant="outlined">
          Unable to create project secret (need OPERATOR membership, or name already used)
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
            helperText="Logical name for deploy"
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
              description="Create git.token here, or link an organization secret as git.token."
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
    </Stack>
  )
}

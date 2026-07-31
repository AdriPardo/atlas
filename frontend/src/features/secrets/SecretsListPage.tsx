import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import VpnKeyOutlinedIcon from '@mui/icons-material/VpnKeyOutlined'
import { Link as RouterLink } from 'react-router-dom'
import { secretsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { RowOverflowMenu } from '../../shared/components/RowOverflowMenu'
import { CloudflareTokenScopesHint } from './CloudflareTokenScopesHint'
import { secretNameHelperText } from './knownSecretHints'

export function SecretsListPage() {
  const queryClient = useQueryClient()
  const [createOpen, setCreateOpen] = useState(false)
  const [name, setName] = useState('')
  const [value, setValue] = useState('')
  const [rotateName, setRotateName] = useState<string | null>(null)
  const [rotateValue, setRotateValue] = useState('')

  const query = useQuery({
    queryKey: ['secrets'],
    queryFn: () => secretsApi.list(),
  })

  const saveMutation = useMutation({
    mutationFn: () => secretsApi.upsert({ name: name.trim(), value }),
    onSuccess: async () => {
      setCreateOpen(false)
      setName('')
      setValue('')
      await queryClient.invalidateQueries({ queryKey: ['secrets'] })
    },
  })

  const rotateMutation = useMutation({
    mutationFn: () => secretsApi.upsert({ name: rotateName!.trim(), value: rotateValue }),
    onSuccess: async () => {
      setRotateName(null)
      setRotateValue('')
      await queryClient.invalidateQueries({ queryKey: ['secrets'] })
    },
  })

  const removeMutation = useMutation({
    mutationFn: (secretId: string) => secretsApi.remove(secretId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['secrets'] })
    },
  })

  const rows = query.data ?? []

  return (
    <PageShell>
      <PageHeader
        title="Organization secrets"
        description="Shared encrypted credentials (ADMIN). Prefer project secrets on each Project for app-specific keys; link these when several projects share the same value."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            New org secret
          </Button>
        }
      />

      <Stack spacing={1.5} sx={{ mb: 2 }}>
        <Alert severity="info" variant="outlined">
          Secrets you store in Atlas (org or project) are injected into customer apps at deploy when
          the repo declares{' '}
          <Box component="code">envFrom.secretRef</Box> in <Box component="code">atlas.yml</Box>.
          Resolution order: project binding alias → project-owned name → organization secret. Prefer
          project-scoped secrets under{' '}
          <Button
            component={RouterLink}
            to="/projects"
            size="small"
            sx={{ verticalAlign: 'baseline', p: 0, minWidth: 0 }}
          >
            Projects
          </Button>
          .
        </Alert>
        <CloudflareTokenScopesHint
          footer={
            <>
              Prefer an org-level <Box component="code">cloudflare.api.token</Box> shared across projects,
              or create/link the same name on each Project.
            </>
          }
        />
      </Stack>

      <DataTableFrame>
        <QueryState isLoading={query.isLoading} isError={query.isError}>
          {rows.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<VpnKeyOutlinedIcon />}
                title="No organization secrets"
                description="Create a shared PAT, API key, or SSH material here, then link it from a project — or create secrets directly on the project for that app."
                actionLabel="New org secret"
                onAction={() => setCreateOpen(true)}
              />
            </Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Scope</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Updated</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((secret) => (
                  <TableRow key={secret.id} hover>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 600 }} className="atlas-mono">
                        {secret.name}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        Organization
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {new Date(secret.createdAt).toLocaleString()}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {new Date(secret.updatedAt).toLocaleString()}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <RowOverflowMenu
                        aria-label={`Actions for ${secret.name}`}
                        items={[
                          {
                            label: 'Rotate value',
                            disabled: rotateMutation.isPending,
                            onClick: () => {
                              setRotateName(secret.name)
                              setRotateValue('')
                            },
                          },
                          {
                            label: 'Delete',
                            destructive: true,
                            disabled: removeMutation.isPending,
                            onClick: () => removeMutation.mutate(secret.id),
                          },
                        ]}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </QueryState>
      </DataTableFrame>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Save organization secret</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {saveMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to save secret (ADMIN only)
              </Alert>
            )}
            <TextField
              label="Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              helperText={secretNameHelperText(
                name,
                'e.g. cloudflare.api.token, shared-github-pat, stripe.secret',
              )}
              fullWidth
              autoFocus
            />
            <TextField
              label="Value"
              value={value}
              onChange={(e) => setValue(e.target.value)}
              type="password"
              fullWidth
              multiline
              minRows={3}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!name.trim() || !value || saveMutation.isPending}
            onClick={() => saveMutation.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>

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
            <TextField label="Name" value={rotateName ?? ''} fullWidth disabled />
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
    </PageShell>
  )
}

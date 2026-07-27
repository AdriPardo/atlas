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

export function SecretsListPage() {
  const queryClient = useQueryClient()
  const [createOpen, setCreateOpen] = useState(false)
  const [name, setName] = useState('')
  const [value, setValue] = useState('')

  const query = useQuery({
    queryKey: ['secrets'],
    queryFn: () => secretsApi.list(),
  })

  const createMutation = useMutation({
    mutationFn: () => secretsApi.create({ name: name.trim(), value }),
    onSuccess: async () => {
      setCreateOpen(false)
      setName('')
      setValue('')
      await queryClient.invalidateQueries({ queryKey: ['secrets'] })
    },
  })

  const rows = query.data ?? []

  return (
    <PageShell>
      <PageHeader
        title="Organization secrets"
        description="Shared encrypted credentials (ADMIN). Prefer project secrets on each Project detail; link these when several projects share the same PAT or key."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            New org secret
          </Button>
        }
      />

      <Alert severity="info" variant="outlined" sx={{ mb: 2 }}>
        Deploy resolves <Box component="code">git.token</Box> as: project binding alias → project-owned
        name → organization secret. Create project-scoped secrets under{' '}
        <Button component={RouterLink} to="/projects" size="small" sx={{ verticalAlign: 'baseline', p: 0, minWidth: 0 }}>
          Projects
        </Button>
        , or link an org secret into a project with alias <Box component="code">git.token</Box>.
      </Alert>

      <DataTableFrame>
        <QueryState isLoading={query.isLoading} isError={query.isError}>
          {rows.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<VpnKeyOutlinedIcon />}
                title="No organization secrets"
                description="Create a shared PAT or SSH key here, then link it from a project — or create secrets directly on the project."
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
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </QueryState>
      </DataTableFrame>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Create organization secret</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {createMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to create secret (ADMIN only; name may already exist)
              </Alert>
            )}
            <TextField
              label="Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              helperText='e.g. "shared-github-pat" — link into projects as git.token'
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
            disabled={!name.trim() || !value || createMutation.isPending}
            onClick={() => createMutation.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </PageShell>
  )
}

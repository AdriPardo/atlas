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
        title="Secrets"
        description="Encrypted credentials for private Git clones (git.token) and SSH host keys."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            New secret
          </Button>
        }
      />

      <Alert severity="info" variant="outlined" sx={{ mb: 2 }}>
        Private repositories need a secret named exactly <Box component="code">git.token</Box> (GitHub
        PAT with <Box component="code">repo</Box> scope). Deploy reads it automatically.
      </Alert>

      <DataTableFrame>
        <QueryState isLoading={query.isLoading} isError={query.isError}>
          {rows.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<VpnKeyOutlinedIcon />}
                title="No secrets yet"
                description="Create git.token for private repos, or an SSH private key PEM for remote hosts."
                actionLabel="New secret"
                onAction={() => setCreateOpen(true)}
              />
            </Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
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
        <DialogTitle>Create secret</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {createMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to create secret (name may already exist)
              </Alert>
            )}
            <TextField
              label="Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              helperText='Use "git.token" for private GitHub clones'
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

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
import { membershipsApi, usersApi } from '../../shared/api/endpoints'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { QueryState } from '../../shared/components/QueryState'
import { RowOverflowMenu } from '../../shared/components/RowOverflowMenu'
import { StatusChip } from '../../shared/components/StatusChip'
import { useAuth } from '../auth/AuthContext'

const ROLE_HELP: Record<string, string> = {
  VIEWER: 'Read project, services, pipelines',
  DEVELOPER: 'Write services/pipelines (no deploy)',
  OPERATOR: 'Deploy, members, alerts scoped to project',
}

export function ProjectMembersPanel({ projectId }: { projectId: string }) {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const queryClient = useQueryClient()
  const [userId, setUserId] = useState('')
  const [role, setRole] = useState('VIEWER')

  const membersQuery = useQuery({
    queryKey: ['projects', projectId, 'memberships'],
    queryFn: () => membershipsApi.list(projectId),
    enabled: !!projectId,
  })

  const usersQuery = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.list(),
    enabled: isAdmin,
  })

  const addMutation = useMutation({
    mutationFn: () => membershipsApi.add(projectId, { userId, role }),
    onSuccess: async () => {
      setUserId('')
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'memberships'] })
    },
  })

  const removeMutation = useMutation({
    mutationFn: (membershipId: string) => membershipsApi.remove(projectId, membershipId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'memberships'] })
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ membershipId, nextRole }: { membershipId: string; nextRole: string }) =>
      membershipsApi.update(projectId, membershipId, { role: nextRole }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'memberships'] })
    },
  })

  const rows = membersQuery.data ?? []
  const users = usersQuery.data ?? []

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
        Members
      </Typography>
      <Typography variant="body2" color="text.secondary">
        VIEWER read · DEVELOPER write services/pipelines · OPERATOR deploy & manage members
      </Typography>
      {isAdmin && (
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }}>
          <TextField
            select
            label="User"
            size="small"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            sx={{ minWidth: 200 }}
          >
            {users.map((u) => (
              <MenuItem key={u.id} value={u.id}>
                {u.username} ({u.role})
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            label="Role"
            size="small"
            value={role}
            onChange={(e) => setRole(e.target.value)}
            sx={{ minWidth: 220 }}
            helperText={ROLE_HELP[role]}
          >
            {['VIEWER', 'DEVELOPER', 'OPERATOR'].map((r) => (
              <MenuItem key={r} value={r}>
                {r}
              </MenuItem>
            ))}
          </TextField>
          <Button
            variant="outlined"
            disabled={!userId || addMutation.isPending}
            onClick={() => addMutation.mutate()}
          >
            Add
          </Button>
        </Stack>
      )}
      {addMutation.isError && (
        <Alert severity="error" variant="outlined">
          Unable to add member (need OPERATOR membership or ADMIN).
        </Alert>
      )}
      <QueryState isLoading={membersQuery.isLoading} isError={membersQuery.isError}>
        {rows.length === 0 ? (
          <EmptyState title="No members" description="Project creator is added as OPERATOR automatically." />
        ) : (
          <DataTableFrame>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>User</TableCell>
                  <TableCell>Role</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((m) => {
                  const username = users.find((u) => u.id === m.userId)?.username
                  return (
                    <TableRow key={m.id}>
                      <TableCell className="atlas-mono">{username || m.userId.slice(0, 8)}</TableCell>
                      <TableCell>
                        {isAdmin ? (
                          <TextField
                            select
                            size="small"
                            value={m.role}
                            disabled={updateMutation.isPending}
                            onChange={(e) =>
                              updateMutation.mutate({ membershipId: m.id, nextRole: e.target.value })
                            }
                            sx={{ minWidth: 140 }}
                          >
                            {['VIEWER', 'DEVELOPER', 'OPERATOR'].map((r) => (
                              <MenuItem key={r} value={r}>
                                {r}
                              </MenuItem>
                            ))}
                          </TextField>
                        ) : (
                          <StatusChip label={m.role} />
                        )}
                      </TableCell>
                      <TableCell align="right">
                        <RowOverflowMenu
                          aria-label={`Actions for member ${m.userId.slice(0, 8)}`}
                          items={[
                            {
                              label: 'Remove',
                              destructive: true,
                              disabled: !isAdmin || removeMutation.isPending,
                              onClick: () => removeMutation.mutate(m.id),
                            },
                          ]}
                        />
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </DataTableFrame>
        )}
      </QueryState>
    </Stack>
  )
}
